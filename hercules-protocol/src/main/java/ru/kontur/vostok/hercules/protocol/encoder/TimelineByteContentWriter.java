package ru.kontur.vostok.hercules.protocol.encoder;

import ru.kontur.vostok.hercules.protocol.TimelineByteContent;

public class TimelineByteContentWriter implements Writer<TimelineByteContent> {

    private static final TimelineReadStateWriter stateWriter = new TimelineReadStateWriter();
    private static final ArrayWriter<byte[]> arrayWriter = new ArrayWriter<>(Encoder::writeRawBytes);

    @Override
    public void write(Encoder encoder, TimelineByteContent value) {
        stateWriter.write(encoder, value.getReadState());
        arrayWriter.write(encoder, value.getRawEvents());
    }
}
