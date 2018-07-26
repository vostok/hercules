package ru.kontur.vostok.hercules.elastic.adapter;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Daniil Zhenikhov
 */
public class ApiKeysParserTest {
    private static final Map<String, List<String>> expected;
    private static final InputStream inputStream;

    static {
        expected = new HashMap<>();

        expected.put("test-HashOfSomething", Arrays.asList("test-*-??", "test-??"));
        expected.put("new-test-HashOfNewTest", Arrays.asList("test-mew-??-*-?", "new-????"));

        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        inputStream = loader.getResourceAsStream("api-keys.yml");
    }

    @Test
    public void shouldCorrectParse() throws IOException {
        Assert.assertEquals(expected, ApiKeysParser.parse(inputStream));
    }
}
