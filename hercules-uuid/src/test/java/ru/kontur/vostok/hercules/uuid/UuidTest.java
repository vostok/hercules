package ru.kontur.vostok.hercules.uuid;

import org.junit.Test;
import ru.kontur.vostok.hercules.util.time.TimeUtil;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

/**
 * @author Gregory Koshelev
 */
public class UuidTest {
    @Test
    public void testTimestampExtraction() {
        UUID uuid = UUID.fromString("50554d6e-29bb-11e5-b345-feff819cdc9f");
        long ticks = uuid.timestamp();
        long timestamp = TimeUtil.gregorianTicksToUnixMillis(ticks);
        Instant instant = Instant.ofEpochMilli(timestamp);
        OffsetDateTime date = OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
        assertEquals(2015, date.getYear());
        assertEquals(7, date.getMonthValue());
        assertEquals(14, date.getDayOfMonth());
    }
}
