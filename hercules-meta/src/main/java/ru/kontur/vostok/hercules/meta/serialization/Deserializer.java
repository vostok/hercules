package ru.kontur.vostok.hercules.meta.serialization;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

import java.io.IOException;

/**
 * @author Gregory Koshelev
 */
public final class Deserializer {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final ObjectReader reader;

    private Deserializer(ObjectReader reader) {
        this.reader = reader;
    }

    public <T> T deserialize(byte[] data) throws DeserializationException {
        try {
            return reader.readValue(data);
        } catch (IOException ex) {
            throw new DeserializationException("Deserializaion failed with Exception", ex);
        }
    }

    public static Deserializer forClass(Class<?> clazz) {
        return new Deserializer(OBJECT_MAPPER.readerFor(clazz));
    }
}
