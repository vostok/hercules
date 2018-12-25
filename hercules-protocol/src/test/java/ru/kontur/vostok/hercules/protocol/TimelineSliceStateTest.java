package ru.kontur.vostok.hercules.protocol;

import org.junit.Test;
import ru.kontur.vostok.hercules.protocol.decoder.TimelineSliceStateReader;
import ru.kontur.vostok.hercules.protocol.encoder.TimelineSliceStateWriter;
import ru.kontur.vostok.hercules.util.EventUtil;

import java.util.UUID;

public class TimelineSliceStateTest {

    @Test
    public void shouldWriteRead() {
        WriteReadPipe
                .init(new TimelineSliceStateWriter(), new TimelineSliceStateReader())
                .process(new TimelineSliceState(1, 123456789L, EventUtil.eventIdAsBytes(1, new UUID(1, 2))))
                .assertEquals(HerculesProtocolAssert::assertEquals);
    }
}
