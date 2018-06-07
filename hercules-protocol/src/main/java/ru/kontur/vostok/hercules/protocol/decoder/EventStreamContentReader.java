package ru.kontur.vostok.hercules.protocol.decoder;

import ru.kontur.vostok.hercules.protocol.EventStreamContent;
import ru.kontur.vostok.hercules.protocol.StreamReadState;

public class EventStreamContentReader {

    public static EventStreamContent read(Decoder decoder) {
        StreamReadState readState = StreamReadStateReader.read(decoder);
        int count = decoder.readInteger();
        String[] records = new String[count];
        for (int i = 0; i < count; ++i) {
            records[i] = decoder.readText();
        }
        return new EventStreamContent(readState, records);
    }
}
