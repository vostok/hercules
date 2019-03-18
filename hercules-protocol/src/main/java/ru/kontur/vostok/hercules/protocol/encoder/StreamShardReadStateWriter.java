package ru.kontur.vostok.hercules.protocol.encoder;

import ru.kontur.vostok.hercules.protocol.StreamShardReadState;

public class StreamShardReadStateWriter implements Writer<StreamShardReadState> {

    @Override
    public void write(Encoder encoder, StreamShardReadState shardReadState) {
        encoder.writeInteger(shardReadState.getPartition());
        encoder.writeLong(shardReadState.getOffset());
    }
}
