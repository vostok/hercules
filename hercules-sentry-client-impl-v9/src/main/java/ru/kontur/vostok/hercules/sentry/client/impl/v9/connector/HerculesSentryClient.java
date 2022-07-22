package ru.kontur.vostok.hercules.sentry.client.impl.v9.connector;

import io.sentry.SentryClient;
import io.sentry.connection.Connection;
import io.sentry.context.ContextManager;
import io.sentry.event.Event;
import io.sentry.event.helper.ShouldSendEventCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

/**
 * Custom Sentry client, for sending {@link Event}s to a Sentry server.
 *
 * @author Petr Demenev
 */
public class HerculesSentryClient extends SentryClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(HerculesSentryClient.class);

    /**
     * The underlying {@link Connection} to use for sending events to Sentry.
     */
    private final Connection connection;
    /**
     * Set of callbacks that are checked before each {@link Event} is sent to Sentry.
     */
    private final Set<ShouldSendEventCallback> shouldSendEventCallbacks = new HashSet<>();

    /**
     * Constructs a {@link SentryClient} instance using the provided connection.
     * <p>
     * Note that the most recently constructed instance is stored statically, so it can be used with
     * the static helper methods.
     *
     * @param connection     Underlying {@link Connection} instance to use for sending events
     * @param contextManager {@link ContextManager} instance to use for storing contextual data
     */
    public HerculesSentryClient(Connection connection, ContextManager contextManager) {
        super(connection, contextManager);
        this.connection = connection;
    }

    /**
     * Sends a built {@link Event} to the Sentry server.
     *
     * @param event event to send to Sentry.
     */
    @Override
    public void sendEvent(Event event) {
        for (ShouldSendEventCallback shouldSendEventCallback : shouldSendEventCallbacks) {
            if (!shouldSendEventCallback.shouldSend(event)) {
                LOGGER.trace("Not sending Event because of ShouldSendEventCallback: {}", shouldSendEventCallback);
                return;
            }
        }

        try {
            if (connection instanceof HerculesHttpConnection) {
                ((HerculesHttpConnection) connection).sendWithoutLocking(event);
            } else {
                connection.send(event);
            }
        } finally {
            getContext().setLastEventId(event.getId());
        }
    }

    /**
     * Add a callback that is called before an {@link Event} is sent to Sentry.
     *
     * @param shouldSendEventCallback callback instance
     */
    @Override
    public void addShouldSendEventCallback(ShouldSendEventCallback shouldSendEventCallback) {
        shouldSendEventCallbacks.add(shouldSendEventCallback);
    }
}
