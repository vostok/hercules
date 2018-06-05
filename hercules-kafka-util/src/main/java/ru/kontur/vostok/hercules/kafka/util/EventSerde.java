package ru.kontur.vostok.hercules.kafka.util;

import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;
import ru.kontur.vostok.hercules.protocol.Event;

/**
 * @author Gregory Koshelev
 */
public class EventSerde extends WrapperSerde<Event> {
    public EventSerde(Serializer<Event> serializer, Deserializer<Event> deserializer) {
        super(serializer, deserializer);
    }
}
