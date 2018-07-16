package ru.kontur.vostok.hercules.protocol;

import org.junit.Assert;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

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
        Assert.assertEquals(expected.getTtOffset(), actual.getTtOffset());
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
        Assert.assertEquals(expected.getId(), actual.getId());

        assertFieldsEquals(expected, actual);
        Assert.assertArrayEquals(expected.getBytes(), actual.getBytes());
    }

    private static final BiConsumer<Variant, Variant>[] asserters = new BiConsumer[256];
    static {
        Arrays.setAll(asserters, idx -> (expected, actual) -> Assert.assertEquals(expected.getValue(), actual.getValue()));

        asserters[Type.CONTAINER.code] = (expected, actual) -> assertEquals((Container) expected.getValue(), (Container) actual.getValue());
        asserters[Type.STRING.code] = (expected, actual) -> Assert.assertArrayEquals((byte[]) expected.getValue(), (byte[]) actual.getValue());
        asserters[Type.TEXT.code] = (expected, actual) -> Assert.assertArrayEquals((byte[]) expected.getValue(), (byte[]) actual.getValue());

        asserters[Type.BYTE_VECTOR.code] = (expected, actual) -> Assert.assertArrayEquals((byte[]) expected.getValue(), (byte[]) actual.getValue());
        asserters[Type.SHORT_VECTOR.code] = (expected, actual) -> Assert.assertArrayEquals((short[]) expected.getValue(), (short[]) actual.getValue());
        asserters[Type.INTEGER_VECTOR.code] = (expected, actual) -> Assert.assertArrayEquals((int[]) expected.getValue(), (int[]) actual.getValue());
        asserters[Type.LONG_VECTOR.code] = (expected, actual) -> Assert.assertArrayEquals((long[]) expected.getValue(), (long[]) actual.getValue());
        asserters[Type.FLAG_VECTOR.code] = (expected, actual) -> Assert.assertArrayEquals((boolean[]) expected.getValue(), (boolean[]) actual.getValue());
        asserters[Type.FLOAT_VECTOR.code] = (expected, actual) -> Assert.assertArrayEquals((float[]) expected.getValue(), (float[]) actual.getValue(), 0);
        asserters[Type.DOUBLE_VECTOR.code] = (expected, actual) -> Assert.assertArrayEquals((double[]) expected.getValue(), (double[]) actual.getValue(), 0);
        asserters[Type.STRING_VECTOR.code] = (expected, actual) -> Assert.assertArrayEquals((byte[][]) expected.getValue(), (byte[][]) actual.getValue());
        asserters[Type.TEXT_VECTOR.code] = (expected, actual) -> Assert.assertArrayEquals((byte[][]) expected.getValue(), (byte[][]) actual.getValue());

        asserters[Type.BYTE_ARRAY.code] = (expected, actual) -> Assert.assertArrayEquals((byte[]) expected.getValue(), (byte[]) actual.getValue());
        asserters[Type.SHORT_ARRAY.code] = (expected, actual) -> Assert.assertArrayEquals((short[]) expected.getValue(), (short[]) actual.getValue());
        asserters[Type.INTEGER_ARRAY.code] = (expected, actual) -> Assert.assertArrayEquals((int[]) expected.getValue(), (int[]) actual.getValue());
        asserters[Type.LONG_ARRAY.code] = (expected, actual) -> Assert.assertArrayEquals((long[]) expected.getValue(), (long[]) actual.getValue());
        asserters[Type.FLAG_ARRAY.code] = (expected, actual) -> Assert.assertArrayEquals((boolean[]) expected.getValue(), (boolean[]) actual.getValue());
        asserters[Type.FLOAT_ARRAY.code] = (expected, actual) -> Assert.assertArrayEquals((float[]) expected.getValue(), (float[]) actual.getValue(), 0);
        asserters[Type.DOUBLE_ARRAY.code] = (expected, actual) -> Assert.assertArrayEquals((double[]) expected.getValue(), (double[]) actual.getValue(), 0);
        asserters[Type.STRING_ARRAY.code] = (expected, actual) -> Assert.assertArrayEquals((byte[][]) expected.getValue(), (byte[][]) actual.getValue());
        asserters[Type.TEXT_ARRAY.code] = (expected, actual) -> Assert.assertArrayEquals((byte[][]) expected.getValue(), (byte[][]) actual.getValue());
    }

    public static void assertEquals(Variant expected, Variant actual) {
        Assert.assertEquals(expected.getType(), actual.getType());
        asserters[expected.getType().code].accept(expected, actual);
    }

    public static void assertEquals(Container expected, Container actual) {
        assertFieldsEquals(expected, actual);
    }

    private static void assertFieldsEquals(Iterable<Map.Entry<String, Variant>> expected, Iterable<Map.Entry<String, Variant>> actual) {
        Map<String, Variant> expectedMap = StreamSupport.stream(expected.spliterator(), false)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        Map<String, Variant> actualMap = StreamSupport.stream(actual.spliterator(), false)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        Set<String> checked = new HashSet<>();
        for (Map.Entry<String, Variant> entry : expectedMap.entrySet()) {
            String tagName = entry.getKey();
            Variant value = actualMap.get(entry.getKey());
            if (Objects.isNull(value)) {
                Assert.fail("Missing field " + tagName);
            }
            assertEquals(entry.getValue(), value);
            checked.add(tagName);
        }
        for (Map.Entry<String, Variant> entry : actual) {
            String tagName = entry.getKey();
            if (!checked.contains(tagName)) {
                Assert.fail("Extra field " + tagName);
            }
        }
    }
}
