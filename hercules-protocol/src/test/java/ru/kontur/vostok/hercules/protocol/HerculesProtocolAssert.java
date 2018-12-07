package ru.kontur.vostok.hercules.protocol;

import org.junit.Assert;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class HerculesProtocolAssert {

    private static final BiConsumer<Variant, Variant>[] ASSERTERS = new BiConsumer[256];

    static {
        Arrays.setAll(ASSERTERS, idx -> (expected, actual) -> Assert.assertEquals(expected.getValue(), actual.getValue()));

        ASSERTERS[Type.CONTAINER.code] = (expected, actual) -> assertEquals((Container) expected.getValue(), (Container) actual.getValue());
        ASSERTERS[Type.STRING.code] = (expected, actual) -> Assert.assertArrayEquals((byte[]) expected.getValue(), (byte[]) actual.getValue());

        ASSERTERS[Type.VECTOR.code] = (expectedVariant, actualVariant) -> {
            Vector expected = (Vector) expectedVariant.getValue();
            Vector actual = (Vector) actualVariant.getValue();

            Assert.assertEquals(expected.getType(), actual.getType());
            switch (expected.getType()) {
                case CONTAINER:
                    assertArrayEquals(
                            (Container[]) expected.getValue(),
                            (Container[]) actual.getValue(),
                            HerculesProtocolAssert::assertEquals);
                    break;
                case BYTE:
                    Assert.assertArrayEquals((byte[]) expected.getValue(), (byte[]) actual.getValue());
                    break;
                case SHORT:
                    Assert.assertArrayEquals((short[]) expected.getValue(), (short[]) actual.getValue());
                    break;
                case INTEGER:
                    Assert.assertArrayEquals((int[]) expected.getValue(), (int[]) actual.getValue());
                    break;
                case LONG:
                    Assert.assertArrayEquals((long[]) expected.getValue(), (long[]) actual.getValue());
                    break;
                case FLAG:
                    Assert.assertArrayEquals((boolean[]) expected.getValue(), (boolean[]) actual.getValue());
                    break;
                case FLOAT:
                    Assert.assertArrayEquals((float[]) expected.getValue(), (float[]) actual.getValue(), 0);
                    break;
                case DOUBLE:
                    Assert.assertArrayEquals((double[]) expected.getValue(), (double[]) actual.getValue(), 0);
                    break;
                case STRING:
                    Assert.assertArrayEquals((byte[][]) expected.getValue(), (byte[][]) actual.getValue());
                    break;
                case UUID:
                    Assert.assertArrayEquals((java.util.UUID[][]) expected.getValue(), (java.util.UUID[][]) actual.getValue());
                    break;
                case NULL:
                    Assert.assertEquals(((Object[]) expected.getValue()).length, ((Object[]) actual.getValue()).length);
                    break;
                /* TODO: support Vector of Vectors assertion */
                default:
                    Assert.fail("Unsupported type " + expected.getType());
            }
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
        assertEquals(expected, actual, true, true);
    }

    public static void assertEquals(Event expected, Event actual, boolean checkId, boolean checkBytes) {
        Assert.assertEquals(expected.getVersion(), actual.getVersion());
        if (checkId) {
            Assert.assertEquals(expected.getTimestamp(), actual.getTimestamp());
            Assert.assertEquals(expected.getRandom(), actual.getRandom());
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
