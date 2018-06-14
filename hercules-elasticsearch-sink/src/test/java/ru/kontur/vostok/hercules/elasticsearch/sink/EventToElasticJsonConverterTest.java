package ru.kontur.vostok.hercules.elasticsearch.sink;

import org.junit.Test;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.protocol.encoder.EventBuilder;
import ru.kontur.vostok.hercules.util.TimeUtil;

import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.junit.Assert.assertEquals;

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


        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        EventToElasticJsonConverter.formatEvent(stream, event.build());

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
                        "\"Text sample\":\"Test string with json inside {\\\"a\\\": {\\\"b\\\": [123, true, \\\"str\\\"]}}\"" +
                        "}",
                stream.toString()
        );

    }
}
