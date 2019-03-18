package ru.kontur.vostok.hercules.protocol.encoder;

import ru.kontur.vostok.hercules.protocol.ByteStreamContent;


public class ByteStreamContentWriter implements Writer<ByteStreamContent> {

    private static final StreamReadStateWriter STATE_READER = new StreamReadStateWriter();
    private static final ArrayWriter<byte[]> ARRAY_WRITER = new ArrayWriter<>(Encoder::writeRawBytes);

    @Override
    public void write(Encoder encoder, ByteStreamContent byteStreamContent) {
        STATE_READER.write(encoder, byteStreamContent.getState());
        ARRAY_WRITER.write(encoder, byteStreamContent.getEvents());
    }
}
