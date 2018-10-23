package ru.kontur.vostok.hercules.util.parsing;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;
import ru.kontur.vostok.hercules.util.functional.Result;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Set;

public class ParsersTest {

    @Test
    public void shouldParseString() throws Exception {
        Result<String, String> result = Parsers.parseString("abc");
        Assert.assertTrue(result.isOk());
        Assert.assertEquals("abc", result.get());
    }

    @Test
    public void shouldParseInteger() throws Exception {
        Result<Integer, String> result = Parsers.parseInteger("-123");
        Assert.assertTrue(result.isOk());
        Assert.assertEquals(-123, result.get().intValue());

        result = Parsers.parseInteger("abc");
        Assert.assertFalse(result.isOk());
        Assert.assertEquals("Invalid integer 'abc'", result.getError());
    }

    @Test
    public void shouldParseLong() throws Exception {
        Result<Long, String> result = Parsers.parseLong("-123");
        Assert.assertTrue(result.isOk());
        Assert.assertEquals(-123, result.get().longValue());

        result = Parsers.parseLong("abc");
        Assert.assertFalse(result.isOk());
        Assert.assertEquals("Invalid long 'abc'", result.getError());
    }

    @Test
    public void shouldParseList() throws Exception {
        Result<List<Integer>, String> result = Parsers.parseList(Parsers::parseInteger).parse("1, 2, 3");
        Assert.assertTrue(result.isOk());
        Assert.assertThat(result.get(), CoreMatchers.hasItems(1, 2, 3));

        result = Parsers.parseList(Parsers::parseInteger).parse("1, abc, 3");
        Assert.assertFalse(result.isOk());
        Assert.assertEquals("Error at index '1': Invalid integer 'abc'", result.getError());
    }

    @Test
    public void shouldParseSet() throws Exception {
        Result<Set<Integer>, String> result = Parsers.parseSet(Parsers::parseInteger).parse("1, 2, 1");
        Assert.assertTrue(result.isOk());
        Assert.assertThat(result.get(), CoreMatchers.hasItems(1, 2));

        result = Parsers.parseSet(Parsers::parseInteger).parse("1, abc, 3");
        Assert.assertFalse(result.isOk());
        Assert.assertEquals("Error at index '1': Invalid integer 'abc'", result.getError());
    }

    @Test
    public void shouldParseArray() throws Exception {
        Result<Integer[], String> result = Parsers.parseArray(Integer.class, Parsers::parseInteger).parse("1, 2, 3");
        Assert.assertTrue(result.isOk());
        Assert.assertArrayEquals(new Integer[]{1, 2, 3}, result.get());

        result = Parsers.parseArray(Integer.class, Parsers::parseInteger).parse("1, abc, 3");
        Assert.assertFalse(result.isOk());
        Assert.assertEquals("Error at index '1': Invalid integer 'abc'", result.getError());
    }

    @Test
    public void shouldParseArrayOfUrls() throws Exception {
        Result<URL[], String> result = Parsers.parseArray(URL.class, s -> {
            try {
                return Result.ok(new URL(s));
            }
            catch (MalformedURLException e) {
                return Result.error("dummy");
            }
        }).parse("http://test.com");

        Assert.assertTrue(result.isOk());
    }
}
