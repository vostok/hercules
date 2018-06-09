package ru.kontur.vostok.hercules.protocol.encoder;

import ru.kontur.vostok.hercules.protocol.ByteStreamContent;


public class ByteStreamContentWriter {

    public static void write(Encoder encoder, ByteStreamContent byteStreamContent) {
        StreamReadStateWriter.write(encoder, byteStreamContent.getState());
        encoder.writeInteger(byteStreamContent.getEventCount());
        for (byte[] record : byteStreamContent.getEvents()) {
            encoder.writeRawBites(record);
        }
    }
}
