package ru.kontur.vostok.hercules.protocol;

import org.junit.Test;
import ru.kontur.vostok.hercules.protocol.decoder.TimelineStateReader;
import ru.kontur.vostok.hercules.protocol.encoder.TimelineStateWriter;
import ru.kontur.vostok.hercules.protocol.util.EventUtil;

import java.util.UUID;

public class TimelineStateTest {

    @Test
    public void shouldWriteRead() {
        WriteReadPipe.init(new TimelineStateWriter(), new TimelineStateReader())
                .process(new TimelineState(new TimelineSliceState[]{
                        new TimelineSliceState(1, 123_456_789L, EventUtil.eventIdAsBytes(1, new UUID(1, 2))),
                        new TimelineSliceState(2, 123_456_789L, EventUtil.eventIdAsBytes(3, new UUID(3, 4)))
                }))
                .assertEquals(HerculesProtocolAssert::assertEquals);
    }
}
