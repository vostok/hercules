package ru.kontur.vostok.hercules.routing.sentry;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;
import ru.kontur.vostok.hercules.routing.engine.tree.DecisionTreeEngineRoute;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * @author Aleksandr Yuferov
 */
public class SentryRouteDeserializerTest {
    @Test
    public void shouldDeserializeCorrectly() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        SentryRouteDeserializer deserializer = new SentryRouteDeserializer(objectMapper);
        UUID id = UUID.fromString("d4b477cf-79fa-478f-abd0-ead1b484f922");
        byte[] serialized = ("{ " +
                "\"conditions\": { \"properties/project\": \"my-proj\" }, " +
                "\"destination\": { \"organization\": \"sentry-org\", \"project\": \"sentry-project\" }, " +
                "\"description\": \"some description\" " +
                "}")
                .getBytes(StandardCharsets.UTF_8);

        DecisionTreeEngineRoute<SentryDestination> result = deserializer.deserialize(id, serialized);

        Assert.assertEquals(id, result.id());
        Assert.assertNotNull(result.conditions());
        Assert.assertTrue(result.conditions().containsKey("properties/project"));
        Assert.assertEquals("my-proj", result.conditions().get("properties/project"));
        Assert.assertNotNull(result.destination());
        Assert.assertEquals("sentry-org", result.destination().organization());
        Assert.assertEquals("sentry-project", result.destination().project());
        Assert.assertEquals("some description", result.description());
    }
}
