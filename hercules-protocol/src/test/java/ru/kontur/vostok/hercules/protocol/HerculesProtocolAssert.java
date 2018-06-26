package ru.kontur.vostok.hercules.protocol;

import org.junit.Assert;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
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

    public static void assertEquals(StreamReadState expected, StreamReadState actual) {
        Assert.assertEquals(expected.getShardCount(), actual.getShardCount());
        HerculesProtocolAssert.assertArrayEquals(
                expected.getShardStates(),
                actual.getShardStates(),
                HerculesProtocolAssert::assertEquals
        );
    }

    public static void assertEquals(TimelineShardReadState expected, TimelineShardReadState actual) {
        Assert.assertEquals(expected.getShardId(), actual.getShardId());
        Assert.assertEquals(expected.getEventTimestamp(), actual.getEventTimestamp());
        Assert.assertEquals(expected.getEventId(), actual.getEventId());
    }

    public static void assertEquals(TimelineReadState expected, TimelineReadState actual) {
        assertArrayEquals(expected.getShards(), actual.getShards(), HerculesProtocolAssert::assertEquals);
    }

    public static void assertEquals(TimelineContent expected, TimelineContent actual) {
        assertEquals(expected.getReadState(), actual.getReadState());
        assertArrayEquals(expected.getEvents(), actual.getEvents(), HerculesProtocolAssert::assertEquals);
    }

    public static void assertEquals(Event expected, Event actual) {
        Assert.assertEquals(expected.getVersion(), actual.getVersion());
        Assert.assertEquals(expected.getTimestamp(), actual.getTimestamp());

        Map.Entry[] expectedEntries = expected.getTags().entrySet().stream().sorted(Map.Entry.comparingByKey()).toArray(Map.Entry[]::new);
        Map.Entry[] actualEntries = actual.getTags().entrySet().stream().sorted(Map.Entry.comparingByKey()).toArray(Map.Entry[]::new);

        assertArrayEquals(expectedEntries, actualEntries, (ex, ac) -> {
            Assert.assertEquals(ex.getKey(), ac.getKey());
            assertEquals((Variant) ex.getValue(), (Variant) ac.getValue());
        });

        Assert.assertArrayEquals(expected.getBytes(), actual.getBytes());
    }

    private static final BiConsumer<Variant, Variant>[] asserters = new BiConsumer[256];
    static {
        Arrays.setAll(asserters, idx -> (expected, actual) -> Assert.assertEquals(expected.getValue(), actual.getValue()));

        asserters[Type.STRING.value] = (expected, actual) -> Assert.assertArrayEquals((byte[]) expected.getValue(), (byte[]) actual.getValue());
        asserters[Type.TEXT.value] = (expected, actual) -> Assert.assertArrayEquals((byte[]) expected.getValue(), (byte[]) actual.getValue());

        asserters[Type.BYTE_VECTOR.value] = (expected, actual) -> Assert.assertArrayEquals((byte[]) expected.getValue(), (byte[]) actual.getValue());
        asserters[Type.SHORT_VECTOR.value] = (expected, actual) -> Assert.assertArrayEquals((short[]) expected.getValue(), (short[]) actual.getValue());
        asserters[Type.INTEGER_VECTOR.value] = (expected, actual) -> Assert.assertArrayEquals((int[]) expected.getValue(), (int[]) actual.getValue());
        asserters[Type.LONG_VECTOR.value] = (expected, actual) -> Assert.assertArrayEquals((long[]) expected.getValue(), (long[]) actual.getValue());
        asserters[Type.FLAG_VECTOR.value] = (expected, actual) -> Assert.assertArrayEquals((boolean[]) expected.getValue(), (boolean[]) actual.getValue());
        asserters[Type.FLOAT_VECTOR.value] = (expected, actual) -> Assert.assertArrayEquals((float[]) expected.getValue(), (float[]) actual.getValue(), 0);
        asserters[Type.DOUBLE_VECTOR.value] = (expected, actual) -> Assert.assertArrayEquals((double[]) expected.getValue(), (double[]) actual.getValue(), 0);
        asserters[Type.STRING_VECTOR.value] = (expected, actual) -> Assert.assertArrayEquals((byte[][]) expected.getValue(), (byte[][]) actual.getValue());
        asserters[Type.TEXT_VECTOR.value] = (expected, actual) -> Assert.assertArrayEquals((byte[][]) expected.getValue(), (byte[][]) actual.getValue());

        asserters[Type.BYTE_ARRAY.value] = (expected, actual) -> Assert.assertArrayEquals((byte[]) expected.getValue(), (byte[]) actual.getValue());
        asserters[Type.SHORT_ARRAY.value] = (expected, actual) -> Assert.assertArrayEquals((short[]) expected.getValue(), (short[]) actual.getValue());
        asserters[Type.INTEGER_ARRAY.value] = (expected, actual) -> Assert.assertArrayEquals((int[]) expected.getValue(), (int[]) actual.getValue());
        asserters[Type.LONG_ARRAY.value] = (expected, actual) -> Assert.assertArrayEquals((long[]) expected.getValue(), (long[]) actual.getValue());
        asserters[Type.FLAG_ARRAY.value] = (expected, actual) -> Assert.assertArrayEquals((boolean[]) expected.getValue(), (boolean[]) actual.getValue());
        asserters[Type.FLOAT_ARRAY.value] = (expected, actual) -> Assert.assertArrayEquals((float[]) expected.getValue(), (float[]) actual.getValue(), 0);
        asserters[Type.DOUBLE_ARRAY.value] = (expected, actual) -> Assert.assertArrayEquals((double[]) expected.getValue(), (double[]) actual.getValue(), 0);
        asserters[Type.STRING_ARRAY.value] = (expected, actual) -> Assert.assertArrayEquals((byte[][]) expected.getValue(), (byte[][]) actual.getValue());
        asserters[Type.TEXT_ARRAY.value] = (expected, actual) -> Assert.assertArrayEquals((byte[][]) expected.getValue(), (byte[][]) actual.getValue());
    }

    public static void assertEquals(Variant expected, Variant actual) {
        Assert.assertEquals(expected.getType(), actual.getType());
        asserters[expected.getType().value].accept(expected, actual);
    }
}
