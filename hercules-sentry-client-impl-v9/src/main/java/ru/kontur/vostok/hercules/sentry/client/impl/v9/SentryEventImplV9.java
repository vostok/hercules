package ru.kontur.vostok.hercules.sentry.client.impl.v9;

import ru.kontur.vostok.hercules.sentry.client.HerculesSentryEvent;
import io.sentry.event.Event;

/**
 * Contains io.sentry.event.Event
 *
 * @author Tatyana Tokmyanina
 */
public class SentryEventImplV9 implements HerculesSentryEvent {
    private final Event sentryEvent;

    public SentryEventImplV9(Event sentryEvent) {
        this.sentryEvent = sentryEvent;
    }

    public Event getSentryEvent() {
        return sentryEvent;
    }
}
