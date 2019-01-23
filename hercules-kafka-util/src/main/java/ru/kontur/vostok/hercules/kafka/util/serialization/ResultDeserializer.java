package ru.kontur.vostok.hercules.kafka.util.serialization;

import org.apache.kafka.common.serialization.Deserializer;
import ru.kontur.vostok.hercules.util.functional.Result;

import java.util.Map;

/**
 * ResultDeserializer
 *
 * @author Kirill Sulim
 */
public class ResultDeserializer<Type> implements Deserializer<Result<Type, DeserializationException>> {

    private final Deserializer<Type> wrapped;

    public ResultDeserializer(final Deserializer<Type> wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        wrapped.configure(configs, isKey);
    }

    @Override
    public Result<Type, DeserializationException> deserialize(String topic, byte[] data) {
        return Result
            .of(() -> wrapped.deserialize(topic, data))
            .mapError(e -> new DeserializationException(data, e));
    }

    @Override
    public void close() {
        wrapped.close();
    }
}
