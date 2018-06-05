package ru.kontur.vostok.hercules.kafka.util;

import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;

import java.util.Map;

/**
 * @author Gregory Koshelev
 */
public class VoidSerde implements Serde<Void> {
    private final VoidSerializer serializer = new VoidSerializer();
    private final VoidDeserializer deserializer = new VoidDeserializer();

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
    public Serializer<Void> serializer() {
        return serializer;
    }

    @Override
    public Deserializer<Void> deserializer() {
        return deserializer;
    }
}
