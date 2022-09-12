package ru.kontur.vostok.hercules.util.metrics;

import org.junit.Assert;
import org.junit.Test;

/**
 * {@link GraphiteMetricNameSanitizer} unit tests.
 *
 * @author Aleksandr Yuferov
 */
public class GraphiteMetricNameSanitizerTest {

    @Test
    public void shouldNotReplaceCorrectSymbols() {
        var input = "-:_"
                + SanitizerTestUtil.generateStringByRangeClosed('0', '9')
                + SanitizerTestUtil.generateStringByRangeClosed('A', 'Z')
                + SanitizerTestUtil.generateStringByRangeClosed('a', 'z');
        var sanitizer = new GraphiteMetricNameSanitizer();

        String output = sanitizer.sanitize(input);

        Assert.assertEquals(output, input);
    }

    @Test
    public void shouldReplaceIllegalCharacters() {
        var sanitizer = new GraphiteMetricNameSanitizer();

        String output = sanitizer.sanitize("foo.bar");

        Assert.assertEquals("foo_bar", output);
    }
}
