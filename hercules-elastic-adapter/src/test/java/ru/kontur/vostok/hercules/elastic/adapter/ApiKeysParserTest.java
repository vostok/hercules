package ru.kontur.vostok.hercules.elastic.adapter;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import ru.kontur.vostok.hercules.elastic.adapter.parser.ApiKeys;
import ru.kontur.vostok.hercules.elastic.adapter.parser.ApiKeysParser;

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
    public static Map<String, List<String>> expected;
    public static InputStream inputStream;

    @BeforeClass
    public static void setUp() {
        expected = new HashMap<>();

        expected.put("test-HashOfSomething", Arrays.asList("test-*-??", "test-??"));
        expected.put("new-test-HashOfNewTest", Arrays.asList("test-mew-??-*-?", "new-????"));

        ClassLoader loader = ApiKeys.class.getClassLoader();
        inputStream = loader.getResourceAsStream("api-keys.yml");
    }

    @Test
    public void shouldParseCorrect() throws IOException {
        Assert.assertEquals(expected, ApiKeysParser.parse(inputStream));
    }
}
