package ru.kontur.vostok.hercules.protocol;

import org.junit.Test;
import ru.kontur.vostok.hercules.protocol.decoder.Decoder;
import ru.kontur.vostok.hercules.protocol.decoder.StreamShardReadStateReader;
import ru.kontur.vostok.hercules.protocol.encoder.Encoder;
import ru.kontur.vostok.hercules.protocol.encoder.StreamShardReadStateWriter;

import static org.junit.Assert.assertEquals;


public class ShardReadStateWriteReadTest {

    @Test
    public void shouldReadWriteShardReadState() {
        StreamShardReadState shardReadState = new StreamShardReadState(1, 1024);

        Encoder encoder = new Encoder();
        StreamShardReadStateWriter.write(encoder, shardReadState);

        Decoder decoder = new Decoder(encoder.getBytes());
        StreamShardReadState result = StreamShardReadStateReader.read(decoder);

        HerculesProtocolAssert.assertShardReadStateEquals(shardReadState, result);
    }
}
