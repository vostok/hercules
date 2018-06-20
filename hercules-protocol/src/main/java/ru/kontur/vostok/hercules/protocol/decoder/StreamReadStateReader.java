package ru.kontur.vostok.hercules.protocol.decoder;

import ru.kontur.vostok.hercules.protocol.StreamShardReadState;
import ru.kontur.vostok.hercules.protocol.StreamReadState;

public class StreamReadStateReader {

    public static StreamReadState read(Decoder decoder) {
        int count = decoder.readInteger();
        StreamShardReadState[] states = new StreamShardReadState[count];
        for (int i = 0; i < count; ++i) {
            states[i] = StreamShardReadStateReader.read(decoder);
        }
        return new StreamReadState(states);
    }
}
