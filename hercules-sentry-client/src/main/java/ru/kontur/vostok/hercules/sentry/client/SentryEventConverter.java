package ru.kontur.vostok.hercules.sentry.client;

import ru.kontur.vostok.hercules.protocol.Event;

/**
 * Convert Hercules event to SentryEvent.
 *
 * @author Tatyana Tokmyanina
 */
public interface SentryEventConverter {
    SentryEvent convert(Event logEvent);
}
