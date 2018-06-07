package ru.kontur.vostok.hercules.protocol.encoder;

import ru.kontur.vostok.hercules.protocol.ShardReadState;

public class ShardReadStateWriter {

    public static void write(Encoder encoder, ShardReadState shardReadState) {
        encoder.writeInteger(shardReadState.getPartition());
        encoder.writeLong(shardReadState.getOffset());
    }
}
