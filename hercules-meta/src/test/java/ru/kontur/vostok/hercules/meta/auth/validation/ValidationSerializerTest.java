package ru.kontur.vostok.hercules.meta.auth.validation;

import org.junit.Assert;
import org.junit.Test;
import ru.kontur.vostok.hercules.meta.ZnodeValidator;
import ru.kontur.vostok.hercules.meta.filter.Conditions;
import ru.kontur.vostok.hercules.meta.filter.Filter;

/**
 * @author Gregory Koshelev
 */
public class ValidationSerializerTest {
    @Test
    public void shouldSerializeAsValidZnodeName() {
        ValidationSerializer validationSerializer = new ValidationSerializer();
        Validation validation = new Validation("test_apiKey", "stream_test", new Filter[]{new Filter("tag", new Conditions.Exist())});
        String znode = validationSerializer.serialize(validation);
        Assert.assertEquals("test_apiKey.stream_test.%5B%7B%22path%22%3A%22tag%22%2C%22condition%22%3A%7B%22type%22%3A%22exist%22%7D%7D%5D", znode);
        Assert.assertTrue(ZnodeValidator.validate(znode));
    }

    @Test
    public void shouldDeserializeCorrectly() {
        ValidationSerializer validationSerializer = new ValidationSerializer();
        String value = "test_apiKey.stream_test.%5B%7B%22path%22%3A%22tag%22%2C%22condition%22%3A%7B%22type%22%3A%22exist%22%7D%7D%5D";
        Validation validation = validationSerializer.deserialize(value);
        Assert.assertEquals("test_apiKey", validation.getApiKey());
        Assert.assertEquals("stream_test", validation.getStream());
        Assert.assertNotNull(validation.getFilters());
        Assert.assertEquals(1, validation.getFilters().length);
        Filter filter = validation.getFilters()[0];
        Assert.assertEquals("tag", filter.getPath());
        Assert.assertNotNull(filter.getCondition());
        Assert.assertTrue(filter.getCondition() instanceof Conditions.Exist);
    }
}
