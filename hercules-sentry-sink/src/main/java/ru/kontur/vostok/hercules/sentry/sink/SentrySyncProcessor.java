package ru.kontur.vostok.hercules.sentry.sink;

import io.sentry.DefaultSentryClientFactory;
import io.sentry.SentryClient;
import io.sentry.SentryClientFactory;
import org.apache.kafka.streams.processor.AbstractProcessor;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.Type;
import ru.kontur.vostok.hercules.protocol.util.EventUtil;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;

import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Gregory Koshelev
 */
public class SentrySyncProcessor extends AbstractProcessor<UUID, Event> {

    private static final String SENTRY_HOST_PROPERTY = "sentry.host";
    private static final String SENTRY_PORT_PROPERTY = "sentry.port";
    private static final String SENTRY_HTTP_SCHEME_PROPERTY = "sentry.http.scheme";

    private static final String DISABLE_UNCAUGHT_EXCEPTION_HANDLING = DefaultSentryClientFactory.UNCAUGHT_HANDLER_ENABLED_OPTION + "=false";

    public static final String SENTRY_TOKEN_TAG = "sentry-token";

    private final ConcurrentHashMap<String, SentryClient> clients = new ConcurrentHashMap<>();
    private final String sentryHost;
    private final String sentryPort;
    private final String httpScheme;

    public SentrySyncProcessor(Properties properties) {
        this.sentryHost = PropertiesUtil.getRequiredProperty(properties, SENTRY_HOST_PROPERTY, String.class);
        this.sentryPort = PropertiesUtil.getRequiredProperty(properties, SENTRY_PORT_PROPERTY, String.class);
        this.httpScheme = PropertiesUtil.getRequiredProperty(properties, SENTRY_HTTP_SCHEME_PROPERTY, String.class);
    }

    @Override
    public void process(UUID key, Event value) {
        String token = EventUtil.extractRequired(value, SENTRY_TOKEN_TAG, Type.STRING);

        getSentryClient(token).sendEvent(SentryEventConverter.convert(value));
    }

    private SentryClient getSentryClient(String token) {
        return clients.computeIfAbsent(token, s -> SentryClientFactory.sentryClient(tokenToDsn(s)));
    }

    private String tokenToDsn(String key) {
        String[] split = key.split("@");
        if (split.length != 2) {
            throw new IllegalArgumentException(String.format("Key format mismatch. Expected `<token>@<project-id>` but got '%s'", key));
        }
        String token = split[0];
        String projectId = split[1];

        return httpScheme + "://" + token + "@" + sentryHost + ":" + sentryPort + "/" + projectId + "?" + DISABLE_UNCAUGHT_EXCEPTION_HANDLING;
    }
}
