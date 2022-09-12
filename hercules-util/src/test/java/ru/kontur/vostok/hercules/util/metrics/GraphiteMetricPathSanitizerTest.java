package ru.kontur.vostok.hercules.util.metrics;

import org.junit.Assert;
import org.junit.Test;

/**
 * {@link GraphiteMetricPathSanitizer} unit tests.
 *
 * @author Aleksandr Yuferov
 */
public class GraphiteMetricPathSanitizerTest {

    @Test
    public void shouldNotReplaceCorrectSymbols() {
        var input = "-:_."
                + SanitizerTestUtil.generateStringByRangeClosed('0', '9')
                + SanitizerTestUtil.generateStringByRangeClosed('A', 'Z')
                + SanitizerTestUtil.generateStringByRangeClosed('a', 'z');
        var sanitizer = new GraphiteMetricPathSanitizer();

        String output = sanitizer.sanitize(input);

        Assert.assertEquals(output, input);
    }

    @Test
    public void shouldReplaceIllegalCharacters() {
        var sanitizer = new GraphiteMetricPathSanitizer();

        String output = sanitizer.sanitize("foo$bar");

        Assert.assertEquals("foo_bar", output);
    }
}
