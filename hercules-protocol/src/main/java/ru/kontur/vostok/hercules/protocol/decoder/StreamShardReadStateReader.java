package ru.kontur.vostok.hercules.protocol.decoder;

import ru.kontur.vostok.hercules.protocol.StreamShardReadState;

public class StreamShardReadStateReader {

    public static StreamShardReadState read(Decoder decoder) {
        return new StreamShardReadState(decoder.readInteger(), decoder.readLong());
    }

    public static void skip(Decoder decoder) {
        decoder.skipInteger();
        decoder.skipLong();
    }
}
