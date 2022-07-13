package ru.kontur.vostok.hercules.routing.engine.tree;

import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Aleksandr Yuferov
 */
public class DecisionTreeEngineConfigTest {
    @Test
    public void shouldCorrectlyDeserializeFromJson() throws Exception {
        var objectMapper = new ObjectMapper();
        var deserializer = new DecisionTreeEngineConfigDeserializer(objectMapper);
        byte[] serialized = "{ \"allowedTags\": [ \"properties/project\" ] }".getBytes(StandardCharsets.UTF_8);

        DecisionTreeEngineConfig config = deserializer.deserialize(serialized);

        Assert.assertEquals(1, config.allowedTags().size());
        Assert.assertEquals("properties/project", config.allowedTags().get(0).getPath());
    }
}
