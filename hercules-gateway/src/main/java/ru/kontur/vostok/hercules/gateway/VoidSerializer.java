package ru.kontur.vostok.hercules.gateway;

import org.apache.kafka.common.serialization.Serializer;

import java.util.Map;

/**
 * @author Gregory Koshelev
 */
public class VoidSerializer implements Serializer<Void> {
    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {

    }

    @Override
    public byte[] serialize(String topic, Void data) {
        return null;
    }

    @Override
    public void close() {

    }
}
