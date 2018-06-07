package ru.kontur.vostok.hercules.protocol.decoder;

import ru.kontur.vostok.hercules.protocol.ShardReadState;

public class ShardReadStateReader {

    public static ShardReadState read(Decoder decoder) {
        return new ShardReadState(decoder.readInteger(), decoder.readLong());
    }

    public static void skip(Decoder decoder) {
        decoder.skipInteger();
        decoder.skipLong();
    }
}
