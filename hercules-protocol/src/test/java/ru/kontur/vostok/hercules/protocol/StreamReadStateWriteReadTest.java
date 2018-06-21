package ru.kontur.vostok.hercules.protocol;

import org.junit.Test;
import ru.kontur.vostok.hercules.protocol.decoder.StreamReadStateReader;
import ru.kontur.vostok.hercules.protocol.encoder.StreamReadStateWriter;

public class StreamReadStateWriteReadTest {

    @Test
    public void shouldWriteReadStreamReadState() {
        WriteReadPipe
                .init(new StreamReadStateWriter(), new StreamReadStateReader())
                .process(new StreamReadState(new StreamShardReadState[]{
                        new StreamShardReadState(0, 1024),
                        new StreamShardReadState(2, 2028)
                }))
                .assertEquals(HerculesProtocolAssert::assertEquals);
    }
}
