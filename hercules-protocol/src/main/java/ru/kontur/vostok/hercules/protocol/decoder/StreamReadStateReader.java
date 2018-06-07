package ru.kontur.vostok.hercules.protocol.decoder;

import ru.kontur.vostok.hercules.protocol.ShardReadState;
import ru.kontur.vostok.hercules.protocol.StreamReadState;

public class StreamReadStateReader {

    public static StreamReadState read(Decoder decoder) {
        int count = decoder.readInteger();
        ShardReadState[] states = new ShardReadState[count];
        for (int i = 0; i < count; ++i) {
            states[i] = ShardReadStateReader.read(decoder);
        }
        return new StreamReadState(states);
    }
}
