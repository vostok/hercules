package ru.kontur.vostok.hercules.sentry.client;

import ru.kontur.vostok.hercules.protocol.Event;

/**
 * Convert Hercules event to SentryEvent.
 *
 * @author Tatyana Tokmyanina
 */
public interface SentryEventConverter {
    /**
     * Converts Hercules event to Sentry event
     * @param logEvent Hercules event
     * @return converted Sentry event
     */
    HerculesSentryEvent convert(Event logEvent);
}
