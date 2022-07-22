package ru.kontur.vostok.hercules.sentry.client.impl.v9;

import ru.kontur.vostok.hercules.sentry.client.SentryEvent;
import io.sentry.event.Event;

/**
 * @author Tatyana Tokmyanina
 */
public class SentryEventImplV9 implements SentryEvent {
    private final Event sentryEvent;

    public SentryEventImplV9(Event sentryEvent) {
        this.sentryEvent = sentryEvent;
    }

    public Event getSentryEvent() {
        return sentryEvent;
    }
}
