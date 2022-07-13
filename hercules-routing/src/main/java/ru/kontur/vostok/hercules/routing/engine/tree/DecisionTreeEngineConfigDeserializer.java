package ru.kontur.vostok.hercules.routing.engine.tree;

import com.fasterxml.jackson.databind.ObjectMapper;
import ru.kontur.vostok.hercules.routing.engine.EngineConfigDeserializer;

import java.io.IOException;

/**
 * Decision tree router engine configuration deserializer.
 * <p>
 * Implementation of {@link EngineConfigDeserializer} that deserialize {@link DecisionTreeEngineConfig} objects from
 * given data using {@link ObjectMapper}.
 *
 * @author Aleksandr Yuferov
 */
public class DecisionTreeEngineConfigDeserializer implements EngineConfigDeserializer {
    private final ObjectMapper objectMapper;

    public DecisionTreeEngineConfigDeserializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public DecisionTreeEngineConfig deserialize(byte[] data) throws IOException {
        return objectMapper.readValue(data, DecisionTreeEngineConfig.class);
    }
}
