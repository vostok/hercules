package ru.kontur.vostok.hercules.elastic.adapter;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import ru.kontur.vostok.hercules.elastic.adapter.util.IndexResolver;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Daniil Zhenikhov
 */
public class IndexResolverTest {
    private static final String CORRECT_API_KEY = "test-api-key";

    public static IndexResolver indexResolver;
    public static Map<String, List<String>> indexMap;

    @BeforeClass
    public static void setUp() {
        indexMap = new HashMap<>();
        indexMap.put("test-api-key", Collections.singletonList("test-*-????.??.??"));

        indexResolver = new IndexResolver(indexMap);
    }

    @Test
    public void shouldCheckApiKey_WithFilledStarSymbol_ReturnStatusOk() {
        Assert.assertEquals(
                IndexResolver.Status.OK,
                indexResolver.checkIndex(CORRECT_API_KEY, "test-some-fake-info-1002.02.02"));
    }

    @Test
    public void shouldCheckApiKey_WithEmptyStarSymbol_ReturnStatusOk() {
        Assert.assertEquals(
                IndexResolver.Status.OK,
                indexResolver.checkIndex(CORRECT_API_KEY, "test--1002.ab.cd"));
    }

    @Test
    public void shouldCheckApiKey_ReturnStatusUnknown() {
        Assert.assertEquals(
                IndexResolver.Status.UNKNOWN,
                indexResolver.checkIndex("some-api-key", "EMPTY"));
    }

    @Test
    public void shouldCheckApiKey_ReturnsStatusForbidden() {
        Assert.assertEquals(
                IndexResolver.Status.FORBIDDEN,
                indexResolver.checkIndex(CORRECT_API_KEY, "test-as-1922.22.1"));
    }
}
