package ru.kontur.vostok.hercules.protocol;

import org.junit.Assert;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class HerculesProtocolAssert {

    private static final BiConsumer<Variant, Variant>[] ASSERTERS = new BiConsumer[256];

    private static final BiConsumer<Object, Object>[] VECTOR_ASSERTERS = new BiConsumer[256];

    static {
        Arrays.setAll(ASSERTERS, idx -> (expected, actual) -> Assert.assertEquals(expected.getValue(), actual.getValue()));

        ASSERTERS[Type.CONTAINER.code] = (expected, actual) -> assertEquals((Container) expected.getValue(), (Container) actual.getValue());
        ASSERTERS[Type.STRING.code] = (expected, actual) -> Assert.assertArrayEquals((byte[]) expected.getValue(), (byte[]) actual.getValue());

        ASSERTERS[Type.VECTOR.code] = (expectedVariant, actualVariant) -> {
            Vector expected = (Vector) expectedVariant.getValue();
            Vector actual = (Vector) actualVariant.getValue();

            Assert.assertEquals(expected.getType(), actual.getType());
            VECTOR_ASSERTERS[expected.getType().code].accept(expected.getValue(), actual.getValue());
        };
    }

    static {
        Arrays.setAll(VECTOR_ASSERTERS, (idx -> (expected, actual) -> {
            Assert.fail("Unsupported type " + Type.valueOf(idx));
        }));

        VECTOR_ASSERTERS[Type.CONTAINER.code] = (expected, actual) -> {
            assertArrayEquals((Container[])expected, (Container[]) actual, HerculesProtocolAssert::assertEquals);
        };
        VECTOR_ASSERTERS[Type.BYTE.code] = (expected, actual) -> {
            Assert.assertArrayEquals((byte[])expected, (byte[]) actual);
        };
        VECTOR_ASSERTERS[Type.SHORT.code] = (expected, actual) -> {
            Assert.assertArrayEquals((short[])expected, (short[]) actual);
        };
        VECTOR_ASSERTERS[Type.INTEGER.code] = (expected, actual) -> {
            Assert.assertArrayEquals((int[])expected, (int[]) actual);
        };
        VECTOR_ASSERTERS[Type.LONG.code] = (expected, actual) -> {
            Assert.assertArrayEquals((long[])expected, (long[]) actual);
        };
        VECTOR_ASSERTERS[Type.FLAG.code] = (expected, actual) -> {
            Assert.assertArrayEquals((boolean[])expected, (boolean[]) actual);
        };
        VECTOR_ASSERTERS[Type.FLOAT.code] = (expected, actual) -> {
            Assert.assertArrayEquals((float[])expected, (float[]) actual, 0);
        };
        VECTOR_ASSERTERS[Type.DOUBLE.code] = (expected, actual) -> {
            Assert.assertArrayEquals((double[])expected, (double[]) actual, 0);
        };
        VECTOR_ASSERTERS[Type.STRING.code] = (expected, actual) -> {
            Assert.assertArrayEquals((byte[][])expected, (byte[][]) actual);
        };
        VECTOR_ASSERTERS[Type.UUID.code] = (expected, actual) -> {
            Assert.assertArrayEquals((UUID[])expected, (UUID[]) actual);
        };
        VECTOR_ASSERTERS[Type.NULL.code] = (expected, actual) -> {
            Assert.assertArrayEquals((Object[])expected, (Object[]) actual);
        };
        VECTOR_ASSERTERS[Type.VECTOR.code] = (expected, actual) -> {
            assertArrayVectorEquals((Vector[])expected, (Vector[]) actual);
        };
    }

    public static <T> void assertArrayEquals(T[] expected, T[] actual, BiConsumer<T, T> asserter) {
        Assert.assertEquals("Arrays length mismatch", expected.length, actual.length);
        for (int i = 0; i < expected.length; ++i) {
            try {
                asserter.accept(expected[i], actual[i]);
            } catch (AssertionError e) {
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

    public static void assertEquals(TimelineSliceState expected, TimelineSliceState actual) {
        Assert.assertEquals(expected.getSlice(), actual.getSlice());
        Assert.assertEquals(expected.getTtOffset(), actual.getTtOffset());
        Assert.assertArrayEquals(expected.getEventId(), actual.getEventId());
    }

    public static void assertEquals(TimelineState expected, TimelineState actual) {
        assertArrayEquals(expected.getSliceStates(), actual.getSliceStates(), HerculesProtocolAssert::assertEquals);
    }

    public static void assertEquals(TimelineContent expected, TimelineContent actual) {
        assertEquals(expected.getReadState(), actual.getReadState());
        assertArrayEquals(expected.getEvents(), actual.getEvents(), HerculesProtocolAssert::assertEquals);
    }

    public static void assertEquals(Event expected, Event actual) {
        assertEquals(expected, actual, true, true);
    }

    public static void assertEquals(Event expected, Event actual, boolean checkId, boolean checkBytes) {
        Assert.assertEquals(expected.getVersion(), actual.getVersion());
        if (checkId) {
            Assert.assertEquals(expected.getTimestamp(), actual.getTimestamp());
            Assert.assertEquals(expected.getUuid(), actual.getUuid());
        }

        assertTagsEquals(expected.getPayload(), actual.getPayload());
        if (checkBytes) {
            Assert.assertArrayEquals(expected.getBytes(), actual.getBytes());
        }
    }

    public static void assertEquals(Variant expected, Variant actual) {
        Assert.assertEquals(expected.getType(), actual.getType());
        ASSERTERS[expected.getType().code].accept(expected, actual);
    }

    public static void assertEquals(Container expected, Container actual) {
        assertTagsEquals(expected, actual);
    }

    public static void assertVectorEquals(Vector expected, Vector actual) {
        Assert.assertEquals(expected.getType(), actual.getType());
        VECTOR_ASSERTERS[expected.getType().code].accept(expected.getValue(), actual.getValue());
    }

    public static void assertArrayVectorEquals(Vector[] expected, Vector[] actual) {
        Assert.assertEquals(expected.length, actual.length);

    }

    private static void assertTagsEquals
            (Iterable<Map.Entry<String, Variant>> expected, Iterable<Map.Entry<String, Variant>> actual) {
        Map<String, Variant> expectedMap = StreamSupport.stream(expected.spliterator(), false)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        Map<String, Variant> actualMap = StreamSupport.stream(actual.spliterator(), false)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        Set<String> checked = new HashSet<>();
        for (Map.Entry<String, Variant> entry : expectedMap.entrySet()) {
            String tagName = entry.getKey();
            Variant value = actualMap.get(entry.getKey());
            if (Objects.isNull(value)) {
                Assert.fail("Missing tag " + tagName);
            }
            assertEquals(entry.getValue(), value);
            checked.add(tagName);
        }
        for (Map.Entry<String, Variant> entry : actual) {
            String tagName = entry.getKey();
            if (!checked.contains(tagName)) {
                Assert.fail("Extra tag " + tagName);
            }
        }
    }
}
