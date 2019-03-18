package ru.kontur.vostok.hercules.protocol.decoder;

import ru.kontur.vostok.hercules.protocol.StreamShardReadState;

public class StreamShardReadStateReader implements Reader<StreamShardReadState> {

    @Override
    public StreamShardReadState read(Decoder decoder) {
        return new StreamShardReadState(decoder.readInteger(), decoder.readLong());
    }

    @Override
    public int skip(Decoder decoder) {
        int skipped = 0;
        skipped += decoder.skipInteger();
        skipped += decoder.skipLong();
        return skipped;
    }
}
