package ru.kontur.vostok.hercules.kafka.util.serialization;

import org.apache.kafka.common.serialization.Deserializer;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.decoder.EventReader;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * @author Gregory Koshelev
 */
public class EventDeserializer implements Deserializer<Event> {
    private final Set<String> tags;

    public EventDeserializer() {
        this.tags = null;
    }

    public EventDeserializer(Set<String> tags) {
        this.tags = tags;
    }

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {

    }

    @Override
    public Event deserialize(String topic, byte[] data) {
        return EventReader.singleReader(data, tags).read();
    }

    @Override
    public void close() {

    }
}
