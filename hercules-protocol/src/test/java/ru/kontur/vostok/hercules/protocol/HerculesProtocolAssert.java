package ru.kontur.vostok.hercules.protocol;

import java.util.Comparator;

import static org.junit.Assert.assertEquals;


public class HerculesProtocolAssert {

    public static int assertShardReadStateEquals(ShardReadState expected, ShardReadState actual) {
        assertEquals(expected.getPartition(), actual.getPartition());
        assertEquals(expected.getOffset(), actual.getOffset());
        return 0;
    }

    public static void assertStreamReadStateEquals(StreamReadState expected, StreamReadState actual) {
        assertEquals(expected.getShardCount(), actual.getShardCount());
        assertArrayEquals(expected.getShardStates(), actual.getShardStates(), HerculesProtocolAssert::assertShardReadStateEquals);
    }

    public static <T> void assertArrayEquals(T[] expected, T[] actual, Comparator<T> comparator) {
        assertEquals("Arrays length mismatch", expected.length, actual.length);
        for (int i = 0; i < expected.length; ++i) {
            if (0 != comparator.compare(expected[i], actual[i])) {
                throw new AssertionError("Arrays content mismatch at index [" + i + "]");
            }
        }
    }
}
