package ru.kontur.vostok.hercules.management.api;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Vladimir Tsypaev
 */
public class ApiKeyValidationTest {

    @Test
    public void shouldBeCorrectKey() {
        Assert.assertTrue(QueryParameters.KEY.from("key_1234567890123456789abcdefabcdefa").isOk());
        Assert.assertTrue(QueryParameters.KEY.from("key1234567890123456789abcdefabcdefa").isOk());
        Assert.assertTrue(QueryParameters.KEY.from("1234567890123456789abcdefabcdefa").isOk());
    }

    @Test
    public void shouldBeIncorrectKey() {
        Assert.assertTrue("Key should contain uuid at the end of key",
                QueryParameters.KEY.from("key_1").isError());

        Assert.assertTrue("Key should contain uuid without a hyphens at the end of key",
                QueryParameters.KEY.from("key_10c62b70-693d-459f-9eec-3a3c27783460").isError());

        Assert.assertTrue("Key should contain uuid in lowercase at the end of key",
                QueryParameters.KEY.from("key_1234567890123456789ABCDEFABCDEFA").isError());

        Assert.assertTrue("Key should't contain uppercase characters",
                QueryParameters.KEY.from("KEY_123456789012345678901234567890123").isError());

        Assert.assertTrue("Key should't contain non alphanumeric and underscore characters",
                QueryParameters.KEY.from("key$_123456789012345678901234567890123").isError());
    }
}
