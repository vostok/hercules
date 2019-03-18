package ru.kontur.vostok.hercules.protocol.util;

import org.junit.Test;
import ru.kontur.vostok.hercules.protocol.Event;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class EventUtilTest {

    @Test
    public void shouldExtractStringId() throws Exception {
        final Event event = EventBuilder.create()
                .random(UUID.fromString("6e7176f2-f249-43b8-ba54-334cde9d0c23"))
                .timestamp(LocalDateTime.parse("2018-01-15T12:45:00").toEpochSecond(ZoneOffset.UTC) * 100_000_000)
                .build();

        assertEquals("AhqZPjlgzABucXby8klDuLpUM0zenQwj", EventUtil.extractStringId(event));
    }

    @Test
    public void shouldConvertEventIdToHexString() throws Exception {

        final Instant instant = Instant.parse("2019-01-23T00:00:00Z");
        final long ticks = TimeUnit.SECONDS.toNanos(instant.getEpochSecond()) / 100;

        final UUID uuid = UUID.fromString("84a92dee-c471-4dd9-b763-8ef325968d11");

        assertEquals(
            "0x003700CFC016400084A92DEEC4714DD9B7638EF325968D11",
            EventUtil.eventIdAsHexString(ticks, uuid)
        );
    }
}
