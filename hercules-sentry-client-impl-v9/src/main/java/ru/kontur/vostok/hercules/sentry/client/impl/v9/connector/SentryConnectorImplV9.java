package ru.kontur.vostok.hercules.sentry.client.impl.v9.connector;

import io.sentry.SentryClient;
import ru.kontur.vostok.hercules.sentry.client.SentryConnector;

/**
 * Wrapper with {@link SentryClient} or empty
 *
 * @author Petr Demenev
 */
public class SentryConnectorImplV9 implements SentryConnector {
    private final SentryClient sentryClient;

    private SentryConnectorImplV9(SentryClient sentryClient) {
        this.sentryClient = sentryClient;
    }

    private SentryConnectorImplV9() {
        sentryClient = null;
    }

    public static SentryConnectorImplV9 of(SentryClient sentryClient) {
        return new SentryConnectorImplV9(sentryClient);
    }

    public static SentryConnectorImplV9 empty() {
        return new SentryConnectorImplV9();
    }

    public SentryClient getSentryClient() {
        return sentryClient;
    }

    public boolean isPresent() {
        return sentryClient != null;
    }
}
