package ru.kontur.vostok.hercules.elasticsearch.sink;

import org.junit.Test;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.protocol.encoder.EventBuilder;
import ru.kontur.vostok.hercules.util.TimeUtil;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.junit.Assert.assertEquals;
import static ru.kontur.vostok.hercules.util.throwable.ThrowableUtil.toUnchecked;

public class EventToElasticJsonConverterTest {

    @Test
    public void shouldConvertEventToJson() {

        EventBuilder event = new EventBuilder();

        event.setVersion(1);
        Instant instant = LocalDateTime.of(2018, 6, 14, 12, 15, 0).toInstant(ZoneOffset.UTC).plusNanos(123456789);

        event.setTimestamp(instant.getEpochSecond() * TimeUtil.NANOS_IN_SECOND + instant.getNano());

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
                        "\"@timestamp\":\"2018-06-14T12:15:00.123456789Z\"," +
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
    public void shouldConvertEventWithByteVariant() {
        assertVariantConverted("123", Variant.ofByte((byte) 123));
    }

    @Test
    public void shouldConvertEventWithShortVariant() {
        assertVariantConverted("12345", Variant.ofShort((short) 12_345));
    }

    @Test
    public void shouldConvertEventWithIntegerVariant() {
        assertVariantConverted("123456789", Variant.ofInteger(123_456_789));
    }

    @Test
    public void shouldConvertEventWithLongVariant() {
        assertVariantConverted("123456789", Variant.ofLong(123_456_789L));
    }

    @Test
    public void shouldConvertEventWithFloatVariant() {
        assertVariantConverted("0.123456", Variant.ofFloat(0.123456f));
    }

    @Test
    public void shouldConvertEventWithDoubleVariant() {
        assertVariantConverted("0.123456789", Variant.ofDouble(0.123456789));
    }

    @Test
    public void shouldConvertEventWithFlagVariant() {
        assertVariantConverted("true", Variant.ofFlag(true));
        assertVariantConverted("false", Variant.ofFlag(false));
    }

    @Test
    public void shouldConvertEventWithStringVariant() {
        assertVariantConverted("\"Яюё\"", Variant.ofString("Яюё"));
    }

    @Test
    public void shouldConvertEventWithTextVariant() {
        assertVariantConverted("\"Яюё\"", Variant.ofText("Яюё"));
    }

    @Test
    public void shouldConvertEventWithByteArrayVariant() {
        assertVariantConverted("[1,2,3]", Variant.ofByteArray(new byte[]{1, 2, 3}));
    }

    @Test
    public void shouldConvertEventWithShortArrayVariant() {
        assertVariantConverted("[1,2,3]", Variant.ofShortArray(new short[]{1, 2, 3}));
    }

    @Test
    public void shouldConvertEventWithIntegerArrayVariant() {
        assertVariantConverted("[1,2,3]", Variant.ofIntegerArray(new int[]{1, 2, 3}));
    }

    @Test
    public void shouldConvertEventWithLongArrayVariant() {
        assertVariantConverted("[1,2,3]", Variant.ofLongArray(new long[]{1, 2, 3}));
    }

    @Test
    public void shouldConvertEventWithFloatArrayVariant() {
        assertVariantConverted("[1.23,2.34]", Variant.ofFloatArray(new float[]{1.23f, 2.34f}));
    }

    @Test
    public void shouldConvertEventWithDoubleArrayVariant() {
        assertVariantConverted("[1.23,2.34]", Variant.ofDoubleArray(new double[]{1.23, 2.34}));
    }

    @Test
    public void shouldConvertEventWithFlagArrayVariant() {
        assertVariantConverted("[true,false]", Variant.ofFlagArray(new boolean[]{true, false}));
    }

    @Test
    public void shouldConvertEventWithStringArrayVariant() {
        assertVariantConverted("[\"Абв\",\"Ежз\"]", Variant.ofStringArray(new String[]{"Абв", "Ежз"}));
    }

    @Test
    public void shouldConvertEventWithTextArrayVariant() {
        assertVariantConverted("[\"Абв\",\"Ежз\"]", Variant.ofTextArray(new String[]{"Абв", "Ежз"}));
    }

    private void assertVariantConverted(String convertedVariant, Variant variant) {
        EventBuilder builder = new EventBuilder();
        builder.setTag("v", variant);

        assertEquals("{\"@timestamp\":\"1970-01-01T00:00:00Z\",\"v\":" + convertedVariant + "}", builderToJson(builder));
    }

    private static String builderToJson(EventBuilder builder) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        EventToElasticJsonConverter.formatEvent(stream, builder.build());
        return toUnchecked(() -> stream.toString(StandardCharsets.UTF_8.name()));
    }
}
