package ru.kontur.vostok.hercules.sentry.client.impl;

import io.sentry.SentryEvent;
import ru.kontur.vostok.hercules.sentry.client.HerculesSentryEvent;

/**
 * Contains io.sentry.SentryEvent
 *
 * @author Tatyana Tokmyanina
 */
public class SentryEventImpl implements HerculesSentryEvent {
    private final SentryEvent sentryEvent;

    public SentryEventImpl(SentryEvent sentryEvent) {
        this.sentryEvent = sentryEvent;
    }

    public SentryEvent getSentryEvent() {
        return sentryEvent;
    }
}
