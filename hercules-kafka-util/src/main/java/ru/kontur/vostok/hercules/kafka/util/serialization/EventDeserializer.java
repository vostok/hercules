package ru.kontur.vostok.hercules.kafka.util.serialization;

import org.apache.kafka.common.serialization.Deserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.decoder.Decoder;
import ru.kontur.vostok.hercules.protocol.decoder.EventReader;
import ru.kontur.vostok.hercules.util.bytes.ByteUtil;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * @author Gregory Koshelev
 */
public class EventDeserializer implements Deserializer<Event> {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventDeserializer.class);

    private final Set<String> tags;

    private EventDeserializer(Set<String> tags) {
        this.tags = tags;
    }

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {

    }

    @Override
    public Event deserialize(String topic, byte[] data) {
        try {
            EventReader reader = EventReader.readTags(tags);
            return reader.read(new Decoder(data));
        } catch (Exception e) {
            LOGGER.warn("Error on deserialize bytes '{}'", ByteUtil.bytesToHexString(data), e);
            return null;
        }
    }

    @Override
    public void close() {

    }

    public static EventDeserializer parseNoTags() {
        return new EventDeserializer(Collections.emptySet());
    }

    public static EventDeserializer parseAllTags() {
        return new EventDeserializer(null);
    }

    public static EventDeserializer parseTags(Set<String> tags) {
        Objects.requireNonNull(tags);
        return new EventDeserializer(tags);
    }
}
