package ru.kontur.vostok.hercules.partitioner.hash;

import org.junit.Assert;
import org.junit.Test;
import ru.kontur.vostok.hercules.partitioner.ShardingKey;
import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.EventBuilder;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.protocol.Vector;
import ru.kontur.vostok.hercules.util.time.TimeUtil;

import java.util.UUID;

/**
 * @author Gregory Koshelev
 */
public class NaiveHasherTest {
    @Test
    public void shouldHashTopLevelTags() {
        Event event = EventBuilder.create(TimeUtil.millisToTicks(System.currentTimeMillis()), UUID.randomUUID()).
                tag("byte", Variant.ofByte((byte) 0x01)).
                tag("short", Variant.ofShort((short) 1024)).
                tag("integer", Variant.ofInteger(42)).
                tag("long", Variant.ofLong(0x3333333355555555L)).
                tag("float", Variant.ofFloat(0.125f)).
                tag("double", Variant.ofDouble(0.0625)).
                tag("flag", Variant.ofFlag(true)).
                tag("string", Variant.ofString("abc")).
                tag("uuid", Variant.ofUuid(UUID.fromString("33333333-5555-5555-0000-000000000000"))).
                tag("null", Variant.ofNull()).
                tag("vectorOfBytes", Variant.ofVector(Vector.ofBytes((byte) 0x01, (byte) 0x02))).
                build();

        Hasher hasher = new NaiveHasher();

        Assert.assertEquals(0x01, hasher.hash(event, ShardingKey.fromTag("byte")));
        Assert.assertEquals(1024, hasher.hash(event, ShardingKey.fromTag("short")));
        Assert.assertEquals(42, hasher.hash(event, ShardingKey.fromTag("integer")));
        Assert.assertEquals(0x66666666, hasher.hash(event, ShardingKey.fromTag("long")));
        Assert.assertEquals(0x3e000000, hasher.hash(event, ShardingKey.fromTag("float")));
        Assert.assertEquals(0x3fb00000, hasher.hash(event, ShardingKey.fromTag("double")));
        Assert.assertEquals(2029, hasher.hash(event, ShardingKey.fromTag("flag")));
        Assert.assertEquals('a' * 31 * 31 + 'b' * 31 + 'c', hasher.hash(event, ShardingKey.fromTag("string")));
        Assert.assertEquals(0x66666666, hasher.hash(event, ShardingKey.fromTag("uuid")));
        Assert.assertEquals(0, hasher.hash(event, ShardingKey.fromTag("null")));
        Assert.assertEquals(31 + 0x02, hasher.hash(event, ShardingKey.fromTag("vectorOfBytes")));
    }

    @Test
    public void shouldHashNestedContainers() {
        Event event = EventBuilder.create(System.currentTimeMillis(), UUID.randomUUID()).
                tag(
                        "first",
                        Variant.ofContainer(Container.of(
                                "second",
                                Variant.ofContainer(Container.builder().
                                        tag("integer", Variant.ofInteger(42)).
                                        tag("string", Variant.ofString("a")).
                                        build())))).
                build();

        Hasher hasher = new NaiveHasher();

        Assert.assertEquals(42, hasher.hash(event, ShardingKey.fromKeyPaths("first/second/integer")));
        Assert.assertEquals(42 * 31 + 'a', hasher.hash(event, ShardingKey.fromKeyPaths("first/second/integer", "first/second/string")));
        Assert.assertEquals('a' * 31 + 42, hasher.hash(event, ShardingKey.fromKeyPaths("first/second/string", "first/second/integer")));
    }

    @Test
    public void shouldHashOtherValuesAsZeros() {
        Event event = EventBuilder.create(TimeUtil.millisToTicks(System.currentTimeMillis()), UUID.randomUUID()).
                tag("container", Variant.ofContainer(Container.empty())).
                tag("null", Variant.ofNull()).
                tag("vectorOfShorts", Variant.ofVector(Vector.ofShorts((short) 1, (short) 2))).
                tag("vectorOfIntegers", Variant.ofVector(Vector.ofIntegers(1, 2))).
                tag("vectorOfLongs", Variant.ofVector(Vector.ofLongs(1, 2))).
                tag("vectorOfFlags", Variant.ofVector(Vector.ofFlags(true, false))).
                tag("vectorOfFloats", Variant.ofVector(Vector.ofFloats(1.0f, 2.0f))).
                tag("vectorOfDoubles", Variant.ofVector(Vector.ofDoubles(1.0, 2.0))).
                tag("vectorOfStrings", Variant.ofVector(Vector.ofStrings("a", "b"))).
                tag("vectorOfUuids", Variant.ofVector(Vector.ofUuids(UUID.randomUUID(), UUID.randomUUID()))).
                tag("vectorOfNulls", Variant.ofVector(Vector.ofNulls(null, null))).
                tag("vectorOfContainers", Variant.ofVector(Vector.ofContainers(Container.empty(), Container.empty()))).
                tag("vectorOfVectors", Variant.ofVector(Vector.ofVectors(Vector.ofBytes((byte) 0x01), Vector.ofBytes((byte) 0x02)))).
                build();

        Hasher hasher = new NaiveHasher();

        Assert.assertEquals(0, hasher.hash(event, ShardingKey.fromKeyPaths("container")));
        Assert.assertEquals(0, hasher.hash(event, ShardingKey.fromKeyPaths("null")));
        Assert.assertEquals(0, hasher.hash(event, ShardingKey.fromKeyPaths("null, container")));

        Assert.assertEquals(0, hasher.hash(event, ShardingKey.fromKeyPaths("vectorOfShorts")));
        Assert.assertEquals(0, hasher.hash(event, ShardingKey.fromKeyPaths("vectorOfIntegers")));
        Assert.assertEquals(0, hasher.hash(event, ShardingKey.fromKeyPaths("vectorOfLongs")));
        Assert.assertEquals(0, hasher.hash(event, ShardingKey.fromKeyPaths("vectorOfFlags")));
        Assert.assertEquals(0, hasher.hash(event, ShardingKey.fromKeyPaths("vectorOfFloats")));
        Assert.assertEquals(0, hasher.hash(event, ShardingKey.fromKeyPaths("vectorOfDoubles")));
        Assert.assertEquals(0, hasher.hash(event, ShardingKey.fromKeyPaths("vectorOfStrings")));
        Assert.assertEquals(0, hasher.hash(event, ShardingKey.fromKeyPaths("vectorOfUuids")));
        Assert.assertEquals(0, hasher.hash(event, ShardingKey.fromKeyPaths("vectorOfNulls")));
        Assert.assertEquals(0, hasher.hash(event, ShardingKey.fromKeyPaths("vectorOfContainers")));
        Assert.assertEquals(0, hasher.hash(event, ShardingKey.fromKeyPaths("vectorOfVectors")));
    }

    @Test
    public void shouldHashUndefinedValuesAsZero() {
        Event event = EventBuilder.create(TimeUtil.millisToTicks(System.currentTimeMillis()), UUID.randomUUID()).
                tag("another", Variant.ofInteger(42)).
                build();

        Hasher hasher = new NaiveHasher();

        Assert.assertEquals(0, hasher.hash(event, ShardingKey.fromKeyPaths("undefined")));
        Assert.assertEquals(42, hasher.hash(event, ShardingKey.fromKeyPaths("undefined", "another")));
        Assert.assertEquals(42 * 31, hasher.hash(event, ShardingKey.fromKeyPaths("another", "undefined")));
    }
}
