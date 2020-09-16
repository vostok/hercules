package ru.kontur.vostok.hercules.sentry.sink.client;

import io.sentry.SentryClient;

/**
 * Wrapper with {@link SentryClient} or empty
 *
 * @author Petr Demenev
 */
public class ClientWrapper {
    private final SentryClient sentryClient;

    private ClientWrapper(SentryClient sentryClient) {
        this.sentryClient = sentryClient;
    }

    private ClientWrapper() {
        sentryClient = null;
    }

    public static ClientWrapper of(SentryClient sentryClient) {
        return new ClientWrapper(sentryClient);
    }

    public static ClientWrapper empty() {
        return new ClientWrapper();
    }

    public SentryClient get() {
        return sentryClient;
    }

    public boolean isPresent() {
        return sentryClient != null;
    }
}
