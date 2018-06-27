package ru.kontur.vostok.hercules.protocol.encoder;

import ru.kontur.vostok.hercules.protocol.ByteStreamContent;


public class ByteStreamContentWriter implements Writer<ByteStreamContent> {

    private static final StreamReadStateWriter stateReader = new StreamReadStateWriter();
    private static final ArrayWriter<byte[]> arrayWriter = new ArrayWriter<>(Encoder::writeRawBytes);

    @Override
    public void write(Encoder encoder, ByteStreamContent byteStreamContent) {
        stateReader.write(encoder, byteStreamContent.getState());
        arrayWriter.write(encoder, byteStreamContent.getEvents());
    }
}
