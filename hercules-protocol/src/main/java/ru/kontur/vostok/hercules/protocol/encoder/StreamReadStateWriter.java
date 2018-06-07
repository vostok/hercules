package ru.kontur.vostok.hercules.protocol.encoder;

import ru.kontur.vostok.hercules.protocol.ShardReadState;
import ru.kontur.vostok.hercules.protocol.StreamReadState;

public class StreamReadStateWriter {

    public static void write(Encoder encoder, StreamReadState streamReadState) {
        encoder.writeInteger(streamReadState.getShardCount());
        for (ShardReadState shardReadState : streamReadState.getShardStates()) {
            ShardReadStateWriter.write(encoder, shardReadState);
        }
    }
}
