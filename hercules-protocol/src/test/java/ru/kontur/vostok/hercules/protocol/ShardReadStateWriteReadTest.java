package ru.kontur.vostok.hercules.protocol;

import org.junit.Test;
import ru.kontur.vostok.hercules.protocol.decoder.Decoder;
import ru.kontur.vostok.hercules.protocol.decoder.ShardReadStateReader;
import ru.kontur.vostok.hercules.protocol.encoder.Encoder;
import ru.kontur.vostok.hercules.protocol.encoder.ShardReadStateWriter;

import static org.junit.Assert.assertEquals;


public class ShardReadStateWriteReadTest {

    @Test
    public void shouldReadWriteShardReadState() {
        ShardReadState shardReadState = new ShardReadState(1, 1024);

        Encoder encoder = new Encoder();
        ShardReadStateWriter.write(encoder, shardReadState);

        Decoder decoder = new Decoder(encoder.getBytes());
        ShardReadState result = ShardReadStateReader.read(decoder);

        HerculesProtocolAssert.assertShardReadStateEquals(shardReadState, result);
    }
}
