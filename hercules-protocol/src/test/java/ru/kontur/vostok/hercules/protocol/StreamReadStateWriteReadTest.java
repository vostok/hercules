package ru.kontur.vostok.hercules.protocol;

import org.junit.Test;
import ru.kontur.vostok.hercules.protocol.decoder.Decoder;
import ru.kontur.vostok.hercules.protocol.decoder.StreamReadStateReader;
import ru.kontur.vostok.hercules.protocol.encoder.Encoder;
import ru.kontur.vostok.hercules.protocol.encoder.StreamReadStateWriter;

public class StreamReadStateWriteReadTest {

    @Test
    public void shouldWriteReadStreamReadState() {
        StreamReadState streamReadState = new StreamReadState(new StreamShardReadState[]{
                new StreamShardReadState(0, 1024),
                new StreamShardReadState(2, 2028)
        });

        Encoder encoder = new Encoder();
        StreamReadStateWriter.write(encoder, streamReadState);

        Decoder decoder = new Decoder(encoder.getBytes());
        StreamReadState result = StreamReadStateReader.read(decoder);

        HerculesProtocolAssert.assertStreamReadStateEquals(streamReadState, result);
    }
}
