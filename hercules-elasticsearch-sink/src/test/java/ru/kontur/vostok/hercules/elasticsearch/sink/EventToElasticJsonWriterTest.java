package ru.kontur.vostok.hercules.elasticsearch.sink;

import org.junit.Test;
import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.protocol.Vector;
import ru.kontur.vostok.hercules.protocol.util.ContainerBuilder;
import ru.kontur.vostok.hercules.protocol.util.EventBuilder;
import ru.kontur.vostok.hercules.util.time.TimeUtil;
import ru.kontur.vostok.hercules.uuid.UuidGenerator;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static ru.kontur.vostok.hercules.util.throwable.ThrowableUtil.toUnchecked;

public class EventToElasticJsonWriterTest {

    @Test
    public void shouldConvertEventToJson() throws Exception {

        EventBuilder event = new EventBuilder();

        event.setVersion(1);
        event.setTimestamp(TimeUtil.gregorianToUnixTicks(137469727200000010L));
        event.setRandom(UuidGenerator.getClientInstance().withTicks(137469727200000010L));

        event.setTag("Byte sample", Variant.ofByte((byte) 127));
        event.setTag("Short sample", Variant.ofShort((short) 10_000));
        event.setTag("Int sample", Variant.ofInteger(123_456_789));
        event.setTag("Long sample", Variant.ofLong(123_456_789L));
        event.setTag("Float sample", Variant.ofFloat(0.123456f));
        event.setTag("Double sample", Variant.ofDouble(0.123456));
        event.setTag("Flag sample", Variant.ofFlag(true));
        event.setTag("Flag sample false", Variant.ofFlag(false));
        event.setTag("String sample", Variant.ofString("Test string with json inside {\"a\": {\"b\": [123, true, \"str\"]}}"));
        event.setTag("Text sample", Variant.ofString("Test string with json inside {\"a\": {\"b\": [123, true, \"str\"]}}"));
        event.setTag("Array sample", Variant.ofVector(Vector.ofIntegers(new int[]{1, 2, 3})));

        assertEquals(
                "{" +
                        "\"@timestamp\":\"2018-05-30T11:32:00.000001000Z\"," +
                        "\"Byte sample\":127," +
                        "\"Short sample\":10000," +
                        "\"Int sample\":123456789," +
                        "\"Long sample\":123456789," +
                        "\"Float sample\":0.123456," +
                        "\"Double sample\":0.123456," +
                        "\"Flag sample\":true," +
                        "\"Flag sample false\":false," +
                        "\"String sample\":\"Test string with json inside {\\\"a\\\": {\\\"b\\\": [123, true, \\\"str\\\"]}}\"," +
                        "\"Text sample\":\"Test string with json inside {\\\"a\\\": {\\\"b\\\": [123, true, \\\"str\\\"]}}\"," +
                        "\"Array sample\":[1,2,3]" +
                        "}",
                builderToJson(event)

        );
    }

    @Test
    public void shouldConvertEventWithByteVariant() throws Exception {
        assertVariantConverted("123", Variant.ofByte((byte) 123));
    }

    @Test
    public void shouldConvertEventWithShortVariant() throws Exception {
        assertVariantConverted("12345", Variant.ofShort((short) 12_345));
    }

    @Test
    public void shouldConvertEventWithIntegerVariant() throws Exception {
        assertVariantConverted("123456789", Variant.ofInteger(123_456_789));
    }

    @Test
    public void shouldConvertEventWithLongVariant() throws Exception {
        assertVariantConverted("123456789", Variant.ofLong(123_456_789L));
    }

    @Test
    public void shouldConvertEventWithFloatVariant() throws Exception {
        assertVariantConverted("0.123456", Variant.ofFloat(0.123456f));
    }

    @Test
    public void shouldConvertEventWithDoubleVariant() throws Exception {
        assertVariantConverted("0.123456789", Variant.ofDouble(0.123456789));
    }

    @Test
    public void shouldConvertEventWithFlagVariant() throws Exception {
        assertVariantConverted("true", Variant.ofFlag(true));
        assertVariantConverted("false", Variant.ofFlag(false));
    }

    @Test
    public void shouldConvertEventWithStringVariant() throws Exception {
        assertVariantConverted("\"Яюё\"", Variant.ofString("Яюё"));
    }

    @Test
    public void shouldConvertEventWithUuidVariant() throws Exception {
        assertVariantConverted(
                "\"11203800-63fd-11e8-83e2-3a587d902000\"",
                Variant.ofUuid(UUID.fromString("11203800-63fd-11e8-83e2-3a587d902000")));
    }

    @Test
    public void shouldConvertEventWithNullVariant() throws Exception {
        assertVariantConverted(null, Variant.ofNull());
    }

    @Test
    public void shouldConvertEventWithByteVectorVariant() throws Exception {
        assertVariantConverted("[1,2,3]", Variant.ofVector(Vector.ofBytes(new byte[]{1, 2, 3})));
    }

    @Test
    public void shouldConvertEventWithShortVectorVariant() throws Exception {
        assertVariantConverted("[1,2,3]", Variant.ofVector(Vector.ofShorts(new short[]{1, 2, 3})));
    }

    @Test
    public void shouldConvertEventWithIntegerVectorVariant() throws Exception {
        assertVariantConverted("[1,2,3]", Variant.ofVector(Vector.ofIntegers(new int[]{1, 2, 3})));
    }

    @Test
    public void shouldConvertEventWithLongVectorVariant() throws Exception {
        assertVariantConverted("[1,2,3]", Variant.ofVector(Vector.ofLongs(new long[]{1, 2, 3})));
    }

    @Test
    public void shouldConvertEventWithFloatVectorVariant() throws Exception {
        assertVariantConverted("[1.23,2.34]", Variant.ofVector(Vector.ofFloats(new float[]{1.23f, 2.34f})));
    }

    @Test
    public void shouldConvertEventWithDoubleVectorVariant() throws Exception {
        assertVariantConverted("[1.23,2.34]", Variant.ofVector(Vector.ofDoubles(new double[]{1.23, 2.34})));
    }

    @Test
    public void shouldConvertEventWithFlagVectorVariant() throws Exception {
        assertVariantConverted("[true,false]", Variant.ofVector(Vector.ofFlags(new boolean[]{true, false})));
    }

    @Test
    public void shouldConvertEventWithStringVectorVariant() throws Exception {
        assertVariantConverted("[\"Абв\",\"Ежз\"]", Variant.ofVector(Vector.ofStrings(new String[]{"Абв", "Ежз"})));
    }

    @Test
    public void shouldConvertEventWithUuidVectorVariant() throws Exception {
        assertVariantConverted(
                "[\"11203800-63fd-11e8-83e2-3a587d902000\",\"05bd046a-ecc0-11e8-8eb2-f2801f1b9fd1\"]",
                Variant.ofVector(Vector.ofUuids(new UUID[]{
                        UUID.fromString("11203800-63fd-11e8-83e2-3a587d902000"), UUID.fromString("05bd046a-ecc0-11e8-8eb2-f2801f1b9fd1")})));
    }

    @Test
    public void shouldConvertEventWithNullVectorVariant() throws Exception {
        assertVariantConverted("[null,null]", Variant.ofVector(Vector.ofNulls(new Object[]{null, null})));
    }

    @Test
    public void shouldConvertEventWithVectorOfVectorsVariant() throws Exception {
        assertVariantConverted(
                "[[1,2],[3,4]]",
                Variant.ofVector(Vector.ofVectors(new Vector[]{
                        Vector.ofIntegers(new int[]{1, 2}),
                        Vector.ofIntegers(new int[]{3, 4})})));
    }

    @Test
    public void shouldWriteContainer() throws Exception {
        ContainerBuilder containerBuilder = ContainerBuilder.create();
        containerBuilder.tag("a", Variant.ofInteger(123));

        assertVariantConverted(
                "{\"a\":123}",
                Variant.ofContainer(containerBuilder.build())
        );
    }

    @Test
    public void shouldWriteNestedContainer() throws Exception {
        ContainerBuilder nested = ContainerBuilder.create();
        nested.tag("a", Variant.ofInteger(123));

        ContainerBuilder wrapper = ContainerBuilder.create();
        wrapper.tag("nested", Variant.ofContainer(nested.build()));


        assertVariantConverted(
                "{\"nested\":{\"a\":123}}",
                Variant.ofContainer(wrapper.build())
        );
    }

    @Test
    public void shouldWriteVectorOfContainers() throws Exception {
        ContainerBuilder first = ContainerBuilder.create();
        first.tag("a", Variant.ofInteger(123));

        ContainerBuilder second = ContainerBuilder.create();
        second.tag("b", Variant.ofInteger(456));

        assertVariantConverted(
                "[{\"a\":123},{\"b\":456}]",
                Variant.ofVector(Vector.ofContainers(new Container[]{first.build(), second.build()}))
        );
    }

    private void assertVariantConverted(String convertedVariant, Variant variant) throws Exception {
        EventBuilder builder = new EventBuilder();
        builder.setTimestamp(TimeUtil.UNIX_EPOCH);
        builder.setRandom(UuidGenerator.getClientInstance().withTicks(TimeUtil.unixToGregorianTicks(TimeUtil.UNIX_EPOCH)));
        builder.setTag("v", variant);

        assertEquals("{\"@timestamp\":\"1970-01-01T00:00:00.000000000Z\",\"v\":" + convertedVariant + "}", builderToJson(builder));
    }

    private static String builderToJson(EventBuilder builder) throws Exception {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        EventToElasticJsonWriter.writeEvent(stream, builder.build());
        return toUnchecked(() -> stream.toString(StandardCharsets.UTF_8.name()));
    }
}
