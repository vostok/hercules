package ru.kontur.vostok.hercules.protocol;

import org.junit.Assert;

import java.util.function.BiConsumer;

public class HerculesProtocolAssert {

    public static <T> void assertArrayEquals(T[] expected, T[] actual, BiConsumer<T, T> asserter) {
        Assert.assertEquals("Arrays length mismatch", expected.length, actual.length);
        for (int i = 0; i < expected.length; ++i) {
            try {
                asserter.accept(expected[i], actual[i]);
            }
            catch (AssertionError e) {
                throw new AssertionError("Arrays content mismatch at index [" + i + "]", e);
            }
        }
    }

    public static void assertEquals(StreamShardReadState expected, StreamShardReadState actual) {
        Assert.assertEquals(expected.getPartition(), actual.getPartition());
        Assert.assertEquals(expected.getOffset(), actual.getOffset());
    }

    public static void assertStreamReadStateEquals(StreamReadState expected, StreamReadState actual) {
        Assert.assertEquals(expected.getShardCount(), actual.getShardCount());
        HerculesProtocolAssert.assertArrayEquals(
                expected.getShardStates(),
                actual.getShardStates(),
                HerculesProtocolAssert::assertEquals
        );
    }

    public static void assertEquals(EventId expected, EventId actual) {
        Assert.assertEquals(expected.getP1(), actual.getP1());
        Assert.assertEquals(expected.getP2(), actual.getP2());
    }

    public static void assertEquals(TimelineShardReadState expected, TimelineShardReadState actual) {
        Assert.assertEquals(expected.getShardId(), actual.getShardId());
        Assert.assertEquals(expected.getEventTimestamp(), actual.getEventTimestamp());
        assertEquals(expected.getEventId(), actual.getEventId());
    }

    public static void assertEquals(TimelineReadState expected, TimelineReadState actual) {
        assertArrayEquals(expected.getShards(), actual.getShards(), HerculesProtocolAssert::assertEquals);
    }
}
