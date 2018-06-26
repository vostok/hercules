package ru.kontur.vostok.hercules.protocol;

import org.junit.Assert;
import org.junit.Test;
import ru.kontur.vostok.hercules.protocol.decoder.UUIDReader;
import ru.kontur.vostok.hercules.protocol.encoder.UUIDWriter;

import java.util.UUID;

public class EventIdWriteReadTest {

    @Test
    public void shouldWriteReadEventId() {
        WriteReadPipe
                .init(new UUIDWriter(), new UUIDReader())
                .process(new UUID(1, 2))
                .assertEquals(Assert::assertEquals);
    }
}
