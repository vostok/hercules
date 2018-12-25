package ru.kontur.vostok.hercules.protocol.encoder;

import ru.kontur.vostok.hercules.protocol.TimelineByteContent;

public class TimelineByteContentWriter implements Writer<TimelineByteContent> {

    private static final TimelineStateWriter STATE_WRITER = new TimelineStateWriter();
    private static final ArrayWriter<byte[]> ARRAY_WRITER = new ArrayWriter<>(Encoder::writeRawBytes);

    @Override
    public void write(Encoder encoder, TimelineByteContent value) {
        STATE_WRITER.write(encoder, value.getReadState());
        ARRAY_WRITER.write(encoder, value.getRawEvents());
    }
}
