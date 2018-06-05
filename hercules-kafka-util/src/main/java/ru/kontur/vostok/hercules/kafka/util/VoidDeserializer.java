package ru.kontur.vostok.hercules.kafka.util;

import org.apache.kafka.common.serialization.Deserializer;

import java.util.Map;

/**
 * @author Gregory Koshelev
 */
public class VoidDeserializer implements Deserializer<Void> {
    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {

    }

    @Override
    public Void deserialize(String topic, byte[] data) {
        return null;
    }

    @Override
    public void close() {

    }
}
