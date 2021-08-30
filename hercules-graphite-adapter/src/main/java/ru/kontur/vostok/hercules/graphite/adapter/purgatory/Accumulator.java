package ru.kontur.vostok.hercules.graphite.adapter.purgatory;

import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.util.lifecycle.Lifecycle;

/**
 * Accumulates events into batches and sends to the Gate.
 *
 * @author Gregory Koshelev
 */
public interface Accumulator extends Lifecycle {
    /**
     * Add the event to the accumulator.
     * <p>
     * The event will be sent to the Gate to the specified stream.
     *
     * @param stream the stream name
     * @param event  the event
     */
    void add(String stream, Event event);
}
