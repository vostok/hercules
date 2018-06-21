package ru.kontur.vostok.hercules.protocol;

import org.junit.Test;
import ru.kontur.vostok.hercules.protocol.decoder.EventIdReader;
import ru.kontur.vostok.hercules.protocol.encoder.EventIdWriter;

public class EventIdWriteReadTest {

    @Test
    public void shouldWriteReadEventId() {
        WriteReadPipe
                .init(new EventIdWriter(), new EventIdReader())
                .process(new EventId(1, 2))
                .assertEquals(HerculesProtocolAssert::assertEquals);
    }
}
