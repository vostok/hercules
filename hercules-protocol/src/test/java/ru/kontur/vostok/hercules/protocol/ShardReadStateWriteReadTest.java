package ru.kontur.vostok.hercules.protocol;

import org.junit.Test;
import ru.kontur.vostok.hercules.protocol.decoder.StreamShardReadStateReader;
import ru.kontur.vostok.hercules.protocol.encoder.StreamShardReadStateWriter;


public class ShardReadStateWriteReadTest {

    @Test
    public void shouldReadWriteShardReadState() {
        WriteReadPipe
                .init(new StreamShardReadStateWriter(), new StreamShardReadStateReader())
                .process(new StreamShardReadState(1, 1024))
                .assertEquals(HerculesProtocolAssert::assertEquals);
    }
}
