package ru.kontur.vostok.hercules.gateway.client.util;

import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.encoder.ArrayWriter;
import ru.kontur.vostok.hercules.protocol.encoder.Encoder;
import ru.kontur.vostok.hercules.protocol.encoder.EventWriter;

import java.io.ByteArrayOutputStream;

public class EventWriterUtil {
    private static final ArrayWriter<Event> arrayWriter = new ArrayWriter<>(new EventWriter());

    public static byte[] toBytes(int size, Event[] events) {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream(size);
        Encoder encoder = new Encoder(byteStream);
        arrayWriter.write(encoder, events);

        return byteStream.toByteArray();
    }
}
