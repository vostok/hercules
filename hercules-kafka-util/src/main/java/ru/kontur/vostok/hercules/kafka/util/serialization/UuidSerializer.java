package ru.kontur.vostok.hercules.kafka.util.serialization;

import org.apache.kafka.common.serialization.Serializer;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.UUID;

/**
 * @author Gregory Koshelev
 */
public class UuidSerializer implements Serializer<UUID> {
    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {

    }

    @Override
    public byte[] serialize(String topic, UUID data) {
        if (data != null) {
            byte[] bytes = new byte[16];
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            buffer.putLong(data.getMostSignificantBits());
            buffer.putLong(data.getLeastSignificantBits());
            return bytes;
        }
        return null;
    }

    @Override
    public void close() {

    }
}
