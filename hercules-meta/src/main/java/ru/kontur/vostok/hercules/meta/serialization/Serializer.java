package ru.kontur.vostok.hercules.meta.serialization;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

/**
 * @author Gregory Koshelev
 */
public final class Serializer {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final ObjectWriter writer;

    private Serializer(ObjectWriter writer) {
        this.writer = writer;
    }

    public <T> byte[] serialize(T obj) throws SerializationException {
        try {
            return writer.writeValueAsBytes(obj);
        } catch (JsonProcessingException ex) {
            throw new SerializationException("Serialization failed with Exception", ex);
        }
    }

    public static Serializer forClass(Class<?> clazz) {
        return new Serializer(OBJECT_MAPPER.writerFor(clazz));
    }
}
