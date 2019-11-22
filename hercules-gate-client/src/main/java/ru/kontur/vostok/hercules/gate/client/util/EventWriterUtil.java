package ru.kontur.vostok.hercules.gate.client.util;

import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.Sizes;
import ru.kontur.vostok.hercules.protocol.encoder.ArrayWriter;
import ru.kontur.vostok.hercules.protocol.encoder.Encoder;
import ru.kontur.vostok.hercules.protocol.encoder.EventWriter;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

/**
 * @author Daniil Zhenikhov
 */
public class EventWriterUtil {
    private static final ArrayWriter<Event> ARRAY_WRITER = new ArrayWriter<>(new EventWriter());

    /**
     * Encode event array to bytes
     *
     * @param size events size in bytes
     * @param events events to encode
     * @return events are encoded to bytes
     */
    public static byte[] toBytes(int size, Event[] events) {
        ByteBuffer buffer = ByteBuffer.allocate(size + Sizes.SIZE_OF_INTEGER);
        Encoder encoder = new Encoder(buffer);
        ARRAY_WRITER.write(encoder, events);

        return buffer.array();
    }

    /**
     * Encode event array to bytes
     *
     * @param events events to encode
     * @return events are encoded to bytes
     */
    public static byte[] toBytes(Event[] events) {
        return toBytes(calculateSize(events), events);
    }

    private static int calculateSize(Event[] events) {
        int total = 0;

        for (Event event : events) {
            total += event.getBytes().length;
        }

        return total;
    }
}
