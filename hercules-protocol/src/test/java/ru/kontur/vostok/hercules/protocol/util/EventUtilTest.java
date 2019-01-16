package ru.kontur.vostok.hercules.protocol.util;

import org.junit.Test;
import ru.kontur.vostok.hercules.protocol.Event;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

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
}
