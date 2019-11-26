package ru.kontur.vostok.hercules.protocol;

import org.junit.Test;
import ru.kontur.vostok.hercules.protocol.decoder.Decoder;
import ru.kontur.vostok.hercules.protocol.decoder.EventReader;
import ru.kontur.vostok.hercules.protocol.decoder.TimelineContentReader;
import ru.kontur.vostok.hercules.protocol.encoder.Encoder;
import ru.kontur.vostok.hercules.protocol.encoder.TimelineByteContentWriter;
import ru.kontur.vostok.hercules.protocol.encoder.TimelineContentWriter;
import ru.kontur.vostok.hercules.protocol.util.EventUtil;

import java.nio.ByteBuffer;
import java.util.UUID;

public class TimelineContentWriteReadTest {

    @Test
    public void shouldWriteReadTimelineContent() {
        WriteReadPipe<TimelineContent> pipe = WriteReadPipe.init(
                new TimelineContentWriter(),
                new TimelineContentReader(EventReader.readAllTags())
        );

        TimelineContent content = new TimelineContent(
                new TimelineState(new TimelineSliceState[]{
                        new TimelineSliceState(0, 123, EventUtil.eventIdAsBytes(1, new UUID(1, 2))),
                        new TimelineSliceState(1, 456, EventUtil.eventIdAsBytes(3, new UUID(3, 4)))
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
                new TimelineState(new TimelineSliceState[]{
                        new TimelineSliceState(0, 123, EventUtil.eventIdAsBytes(1, new UUID(1, 2))),
                        new TimelineSliceState(1, 456, EventUtil.eventIdAsBytes(3, new UUID(3, 4)))
                }),
                new byte[][]{
                        TestUtil.createEvent().getBytes(),
                        TestUtil.createEvent().getBytes()
                });

        ByteBuffer buffer = ByteBuffer.allocate(byteContent.sizeOf());
        Encoder encoder = new Encoder(buffer);
        new TimelineByteContentWriter().write(encoder, byteContent);

        buffer.flip();
        Decoder decoder = new Decoder(buffer);
        TimelineContent processedContent = new TimelineContentReader(EventReader.readAllTags()).read(decoder);

        TimelineContent expectedContent = new TimelineContent(
                new TimelineState(new TimelineSliceState[]{
                        new TimelineSliceState(0, 123, EventUtil.eventIdAsBytes(1, new UUID(1, 2))),
                        new TimelineSliceState(1, 456, EventUtil.eventIdAsBytes(3, new UUID(3, 4)))
                }),
                new Event[]{
                        TestUtil.createEvent(),
                        TestUtil.createEvent()
                });

        HerculesProtocolAssert.assertEquals(expectedContent, processedContent);
    }
}
