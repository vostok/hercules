package ru.kontur.vostok.hercules.protocol.encoder;

import ru.kontur.vostok.hercules.protocol.StreamShardReadState;
import ru.kontur.vostok.hercules.protocol.StreamReadState;

public class StreamReadStateWriter implements Writer<StreamReadState> {

    private static final ArrayWriter<StreamShardReadState> ARRAY_WRITER = new ArrayWriter<>(new StreamShardReadStateWriter());

    @Override
    public void write(Encoder encoder, StreamReadState streamReadState) {
        ARRAY_WRITER.write(encoder, streamReadState.getShardStates());
    }
}
