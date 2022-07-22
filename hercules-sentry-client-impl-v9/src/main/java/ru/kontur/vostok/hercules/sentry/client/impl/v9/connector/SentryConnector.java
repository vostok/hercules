package ru.kontur.vostok.hercules.sentry.client.impl.v9.connector;

import io.sentry.SentryClient;

/**
 * Wrapper with {@link SentryClient} or empty
 *
 * @author Petr Demenev
 */
public class SentryConnector {
    private final SentryClient sentryClient;

    private SentryConnector(SentryClient sentryClient) {
        this.sentryClient = sentryClient;
    }

    private SentryConnector() {
        sentryClient = null;
    }

    public static SentryConnector of(SentryClient sentryClient) {
        return new SentryConnector(sentryClient);
    }

    public static SentryConnector empty() {
        return new SentryConnector();
    }

    public SentryClient getSentryClient() {
        return sentryClient;
    }

    public boolean isPresent() {
        return sentryClient != null;
    }
}
