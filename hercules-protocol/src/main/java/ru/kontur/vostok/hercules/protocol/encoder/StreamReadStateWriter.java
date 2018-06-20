package ru.kontur.vostok.hercules.protocol.encoder;

import ru.kontur.vostok.hercules.protocol.StreamShardReadState;
import ru.kontur.vostok.hercules.protocol.StreamReadState;

public class StreamReadStateWriter {

    public static void write(Encoder encoder, StreamReadState streamReadState) {
        encoder.writeInteger(streamReadState.getShardCount());
        for (StreamShardReadState shardReadState : streamReadState.getShardStates()) {
            StreamShardReadStateWriter.write(encoder, shardReadState);
        }
    }
}
