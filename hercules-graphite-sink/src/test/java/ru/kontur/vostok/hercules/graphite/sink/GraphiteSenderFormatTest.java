package ru.kontur.vostok.hercules.graphite.sink;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Innokentiy Krivonosov
 */
public class GraphiteSenderFormatTest {
    @Test
    public void format() {
        assertEquals("test 0.000000 1\n", GraphiteSender.format(new GraphiteMetricData("test", 1, 0)));
        assertEquals("test 1.000000 1\n", GraphiteSender.format(new GraphiteMetricData("test", 1, 1)));
        assertEquals("test 1.100000 1\n", GraphiteSender.format(new GraphiteMetricData("test", 1, 1.1)));
        assertEquals("test 1.123457 1\n", GraphiteSender.format(new GraphiteMetricData("test", 1, 1.1234567)));
        assertEquals("test 1.123457 1\n", GraphiteSender.format(new GraphiteMetricData("test", 1, 1.123456789123)));
        assertEquals("test 123456789123.123460 1\n", GraphiteSender.format(new GraphiteMetricData("test", 1, 123456789123.123456789123)));
        assertEquals("test 123456789123456784.000000 1\n", GraphiteSender.format(new GraphiteMetricData("test", 1, 123456789123456789.123)));
        assertEquals("test 123456789123456800000.000000 1\n", GraphiteSender.format(new GraphiteMetricData("test", 1, 123456789123456789123.123)));

        assertEquals("test -1.000000 1\n", GraphiteSender.format(new GraphiteMetricData("test", 1, -1)));
        assertEquals("test -1.100000 1\n", GraphiteSender.format(new GraphiteMetricData("test", 1, -1.1)));
        assertEquals("test -1.123457 1\n", GraphiteSender.format(new GraphiteMetricData("test", 1, -1.1234567)));
        assertEquals("test -1.123457 1\n", GraphiteSender.format(new GraphiteMetricData("test", 1, -1.123456789123)));
        assertEquals("test -123456789123.123460 1\n", GraphiteSender.format(new GraphiteMetricData("test", 1, -123456789123.123456789123)));
        assertEquals("test -123456789123456784.000000 1\n", GraphiteSender.format(new GraphiteMetricData("test", 1, -123456789123456789.123)));
        assertEquals("test -123456789123456800000.000000 1\n", GraphiteSender.format(new GraphiteMetricData("test", 1, -123456789123456789123.123)));
    }
}
