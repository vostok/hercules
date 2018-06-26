package ru.kontur.vostok.hercules.protocol;

import org.junit.Test;
import ru.kontur.vostok.hercules.protocol.decoder.Decoder;
import ru.kontur.vostok.hercules.protocol.decoder.EventReader2;
import ru.kontur.vostok.hercules.protocol.decoder.TimelineContentReader;
import ru.kontur.vostok.hercules.protocol.encoder.Encoder;
import ru.kontur.vostok.hercules.protocol.encoder.TimelineByteContentWriter;
import ru.kontur.vostok.hercules.protocol.encoder.TimelineContentWriter;

import java.util.UUID;

public class TimelineContentWriteReadTest {

    @Test
    public void shouldWriteReadTimelineContent() {
        WriteReadPipe<TimelineContent> pipe = WriteReadPipe.init(
                new TimelineContentWriter(),
                new TimelineContentReader(EventReader2.readAllTags())
        );

        TimelineContent content = new TimelineContent(
                new TimelineReadState(new TimelineShardReadState[]{
                        new TimelineShardReadState(0, 123, new UUID(1, 2)),
                        new TimelineShardReadState(1, 456, new UUID(3, 4))
                }),
                new Event[]{
                        TestUtil.createEvent(),
                        TestUtil.createEvent()
                }
        );

        pipe.process(content).assertEquals(HerculesProtocolAssert::assertEquals);
    }

    @Test
    public void shouldWriteReadTimelineByteContent() {
        TimelineByteContent byteContent = new TimelineByteContent(
                new TimelineReadState(new TimelineShardReadState[]{
                        new TimelineShardReadState(0, 123, new UUID(1, 2)),
                        new TimelineShardReadState(1, 456, new UUID(3, 4))
                }),
                new byte[][]{
                        TestUtil.createEvent().getBytes(),
                        TestUtil.createEvent().getBytes()
                });

        Encoder encoder = new Encoder();
        new TimelineByteContentWriter().write(encoder, byteContent);

        Decoder decoder = new Decoder(encoder.getBytes());
        TimelineContent processedContent = new TimelineContentReader(EventReader2.readAllTags()).read(decoder);

        TimelineContent expectedContent = new TimelineContent(
                new TimelineReadState(new TimelineShardReadState[]{
                        new TimelineShardReadState(0, 123, new UUID(1, 2)),
                        new TimelineShardReadState(1, 456, new UUID(3, 4))
                }),
                new Event[] {
                        TestUtil.createEvent(),
                        TestUtil.createEvent()
                });

        HerculesProtocolAssert.assertEquals(expectedContent, processedContent);
    }
}
