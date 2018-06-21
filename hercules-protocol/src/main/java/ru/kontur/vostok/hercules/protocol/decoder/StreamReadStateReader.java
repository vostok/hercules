package ru.kontur.vostok.hercules.protocol.decoder;

import ru.kontur.vostok.hercules.protocol.StreamShardReadState;
import ru.kontur.vostok.hercules.protocol.StreamReadState;

public class StreamReadStateReader implements Reader<StreamReadState> {

    private static final ArrrayReader<StreamShardReadState> ARRRAY_READER =
            new ArrrayReader<>(new StreamShardReadStateReader(), StreamShardReadState.class);

    @Override
    public StreamReadState read(Decoder decoder) {
        return new StreamReadState(ARRRAY_READER.read(decoder));
    }
}
