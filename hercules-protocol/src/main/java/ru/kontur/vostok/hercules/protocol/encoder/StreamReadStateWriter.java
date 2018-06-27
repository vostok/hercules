package ru.kontur.vostok.hercules.protocol.encoder;

import ru.kontur.vostok.hercules.protocol.StreamShardReadState;
import ru.kontur.vostok.hercules.protocol.StreamReadState;

public class StreamReadStateWriter implements Writer<StreamReadState> {

    private static final ArrayWriter<StreamShardReadState> arrayWriter = new ArrayWriter<>(new StreamShardReadStateWriter());

    @Override
    public void write(Encoder encoder, StreamReadState streamReadState) {
        arrayWriter.write(encoder, streamReadState.getShardStates());
    }
}
