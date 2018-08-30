package ru.kontur.vostok.hercules.elasticsearch.sink;

import org.junit.Test;
import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.protocol.util.ContainerBuilder;
import ru.kontur.vostok.hercules.protocol.util.EventBuilder;
import ru.kontur.vostok.hercules.uuid.UuidGenerator;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static ru.kontur.vostok.hercules.util.throwable.ThrowableUtil.toUnchecked;

public class EventToElasticJsonWriterTest {

    @Test
    public void shouldConvertEventToJson() throws Exception {

        EventBuilder event = new EventBuilder();

        event.setVersion(1);
        event.setEventId(UuidGenerator.getClientInstance().withTicks(137469727200000001L));

        event.setTag("Byte sample", Variant.ofByte((byte) 127));
        event.setTag("Short sample", Variant.ofShort((short) 10_000));
        event.setTag("Int sample", Variant.ofInteger(123_456_789));
        event.setTag("Long sample", Variant.ofLong(123_456_789L));
        event.setTag("Float sample", Variant.ofFloat(0.123456f));
        event.setTag("Double sample", Variant.ofDouble(0.123456));
        event.setTag("Flag sample", Variant.ofFlag(true));
        event.setTag("Flag sample false", Variant.ofFlag(false));
        event.setTag("String sample", Variant.ofString("Test string with json inside {\"a\": {\"b\": [123, true, \"str\"]}}"));
        event.setTag("Text sample", Variant.ofText("Test string with json inside {\"a\": {\"b\": [123, true, \"str\"]}}"));
        event.setTag("Array sample", Variant.ofIntegerArray(new int[]{1, 2, 3}));

        assertEquals(
                "{" +
                        "\"@timestamp\":\"2018-05-30T11:32:00.0000001Z\"," +
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
    public void shouldConvertEventWithTextVariant() throws Exception {
        assertVariantConverted("\"Яюё\"", Variant.ofText("Яюё"));
    }

    @Test
    public void shouldConvertEventWithByteArrayVariant() throws Exception {
        assertVariantConverted("[1,2,3]", Variant.ofByteArray(new byte[]{1, 2, 3}));
    }

    @Test
    public void shouldConvertEventWithShortArrayVariant() throws Exception {
        assertVariantConverted("[1,2,3]", Variant.ofShortArray(new short[]{1, 2, 3}));
    }

    @Test
    public void shouldConvertEventWithIntegerArrayVariant() throws Exception {
        assertVariantConverted("[1,2,3]", Variant.ofIntegerArray(new int[]{1, 2, 3}));
    }

    @Test
    public void shouldConvertEventWithLongArrayVariant() throws Exception {
        assertVariantConverted("[1,2,3]", Variant.ofLongArray(new long[]{1, 2, 3}));
    }

    @Test
    public void shouldConvertEventWithFloatArrayVariant() throws Exception {
        assertVariantConverted("[1.23,2.34]", Variant.ofFloatArray(new float[]{1.23f, 2.34f}));
    }

    @Test
    public void shouldConvertEventWithDoubleArrayVariant() throws Exception {
        assertVariantConverted("[1.23,2.34]", Variant.ofDoubleArray(new double[]{1.23, 2.34}));
    }

    @Test
    public void shouldConvertEventWithFlagArrayVariant() throws Exception {
        assertVariantConverted("[true,false]", Variant.ofFlagArray(new boolean[]{true, false}));
    }

    @Test
    public void shouldConvertEventWithStringArrayVariant() throws Exception {
        assertVariantConverted("[\"Абв\",\"Ежз\"]", Variant.ofStringArray(new String[]{"Абв", "Ежз"}));
    }

    @Test
    public void shouldConvertEventWithTextArrayVariant() throws Exception {
        assertVariantConverted("[\"Абв\",\"Ежз\"]", Variant.ofTextArray(new String[]{"Абв", "Ежз"}));
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
    public void shouldWriteArrayOfContainers() throws Exception {
        ContainerBuilder first = ContainerBuilder.create();
        first.tag("a", Variant.ofInteger(123));

        ContainerBuilder second = ContainerBuilder.create();
        second.tag("b", Variant.ofInteger(456));

        assertVariantConverted(
                "[{\"a\":123},{\"b\":456}]",
                Variant.ofContainerArray(new Container[]{first.build(), second.build()})
        );
    }

    private void assertVariantConverted(String convertedVariant, Variant variant) throws Exception {
        EventBuilder builder = new EventBuilder();
        builder.setEventId(UuidGenerator.getClientInstance().withTicks(0));
        builder.setTag("v", variant);

        assertEquals("{\"@timestamp\":\"1582-10-15T00:00:00Z\",\"v\":" + convertedVariant + "}", builderToJson(builder));
    }

    private static String builderToJson(EventBuilder builder) throws Exception {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        EventToElasticJsonWriter.writeEvent(stream, builder.build());
        return toUnchecked(() -> stream.toString(StandardCharsets.UTF_8.name()));
    }
}
