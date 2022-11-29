package ru.kontur.vostok.hercules.sentry.client;

import ru.kontur.vostok.hercules.kafka.util.processing.BackendServiceFailedException;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.util.routing.SentryDestination;

/**
 * @author Tatyana Tokmyanina
 */
public interface SentryClient {
    /**
     * Try to send the event to Sentry.
     *
     * @param event event to send
     * @param destination Sentry destination where to send event
     * @return true if event is successfully sent
     * otherwise return false
     * @throws BackendServiceFailedException if error occurred
     */
    boolean tryToSend(Event event, SentryDestination destination)
            throws BackendServiceFailedException;
}
