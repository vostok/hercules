package ru.kontur.vostok.hercules.kafka.util.serialization;

import org.apache.kafka.common.serialization.Deserializer;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.UUID;

/**
 * @author Gregory Koshelev
 */
public class UuidDeserializer implements Deserializer<UUID> {
    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {

    }

    @Override
    public UUID deserialize(String topic, byte[] data) {
        if (data != null && data.length == 16) {
            ByteBuffer buffer = ByteBuffer.wrap(data);
            return new UUID(buffer.getLong(), buffer.getLong());
        }
        return null;
    }

    @Override
    public void close() {

    }
}
