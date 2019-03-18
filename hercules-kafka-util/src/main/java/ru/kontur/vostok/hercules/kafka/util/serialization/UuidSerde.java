package ru.kontur.vostok.hercules.kafka.util.serialization;

import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;

import java.util.Map;
import java.util.UUID;

/**
 * @author Gregory Koshelev
 */
public class UuidSerde implements Serde<UUID> {
    private final UuidSerializer serializer = new UuidSerializer();
    private final UuidDeserializer deserializer = new UuidDeserializer();

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        serializer.configure(configs, isKey);
        deserializer.configure(configs, isKey);
    }

    @Override
    public void close() {
        serializer.close();
        deserializer.close();
    }

    @Override
    public Serializer<UUID> serializer() {
        return serializer;
    }

    @Override
    public Deserializer<UUID> deserializer() {
        return deserializer;
    }
}
