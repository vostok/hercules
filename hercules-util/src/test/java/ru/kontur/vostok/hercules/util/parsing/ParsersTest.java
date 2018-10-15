package ru.kontur.vostok.hercules.util.parsing;

import org.junit.Assert;
import org.junit.Test;
import ru.kontur.vostok.hercules.util.functional.Result;

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
}
