package ru.kontur.vostok.hercules.protocol;

import org.junit.Test;
import ru.kontur.vostok.hercules.protocol.decoder.TimelineShardReadStateReader;
import ru.kontur.vostok.hercules.protocol.encoder.TimelineShardReadStateWriter;

import java.util.UUID;

public class TimelineShardReadStateWriteReadTest {

    @Test
    public void shouldWriteReadTimelineShardReadState() {
        WriteReadPipe
                .init(new TimelineShardReadStateWriter(), new TimelineShardReadStateReader())
                .process(new TimelineShardReadState(1, 123456789L, new UUID(1, 2)))
                .assertEquals(HerculesProtocolAssert::assertEquals);
    }
}
