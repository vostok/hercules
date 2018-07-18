package ru.kontur.vostok.hercules.gateway.client.util;

import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.encoder.ArrayWriter;
import ru.kontur.vostok.hercules.protocol.encoder.Encoder;
import ru.kontur.vostok.hercules.protocol.encoder.EventWriter;

import java.io.ByteArrayOutputStream;

/**
 * @author Daniil Zhenikhov
 */
public class EventWriterUtil {
    private static final ArrayWriter<Event> arrayWriter = new ArrayWriter<>(new EventWriter());

    /**
     * Convert array of events to byte array
     *
     * @param size initial capacity for output stream
     * @param events array of events which have to convert
     * @return bytes of events after converting
     */
    public static byte[] toBytes(int size, Event[] events) {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream(size);
        Encoder encoder = new Encoder(byteStream);
        arrayWriter.write(encoder, events);

        return byteStream.toByteArray();
    }

    /**
     * Convert array of events to byte array
     *
     * @param events array of events which have to convert
     * @return bytes of events after converting
     */
    public static byte[] toBytes(Event[] events) {
        return toBytes(calcSize(events), events);
    }

    /**
     * Calculate total size of events' array
     *
     * @return total size
     */
    private static int calcSize(Event[] events) {
        int total = 0;

        for (int i = 0; i < events.length; i++) {
            total += events[i].getBytes().length;
        }

        return total;
    }
}
