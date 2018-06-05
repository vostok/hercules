package ru.kontur.vostok.hercules.kafka.util;

import org.apache.kafka.common.serialization.Serializer;
import ru.kontur.vostok.hercules.protocol.Event;

import java.util.Map;

/**
 * @author Gregory Koshelev
 */
public class EventSerializer implements Serializer<Event> {
    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {

    }

    @Override
    public byte[] serialize(String topic, Event data) {
        if (data == null) {
            return null;
        }
        return data.getBytes();
    }

    @Override
    public void close() {

    }
}
