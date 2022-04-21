package ru.kontur.vostok.hercules.json.format.combiner;

import org.jetbrains.annotations.Nullable;
import ru.kontur.vostok.hercules.protocol.Type;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.util.time.TimeUtil;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Combines timestamp in 100-ns ticks of Unix Epoch and timezone offset (in ticks too)
 * into ISO 8601 string representation in form {@code yyyy-MM-dd'T'HH:mm:ss.nnnnnnnnnXXX}.
 *
 * @author Gregory Koshelev
 */
public class IsoDateTimeCombiner implements Combiner {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.nnnnnnnnnXXX");

    /**
     * Combine timestamp and timezone offset into ISO 8601 formatted date and time string.
     *
     * @param values timestamp (required) and timezone offset (optional)
     * @return ISO 8601 formatted date and time string if timestamp is present, otherwise {@code null}
     */
    @Override
    public @Nullable Object combine(Variant... values) {
        Long timestamp = extractTimestampFrom(values);
        if (timestamp == null) {
            return null;
        }
        long offset = extractOffsetFrom(values);

        ZonedDateTime dateTime = ZonedDateTime.ofInstant(
                TimeUtil.unixTicksToInstant(timestamp),
                ZoneOffset.ofTotalSeconds((int) TimeUtil.ticksToSeconds(offset)));
        return FORMATTER.format(dateTime);
    }

    private Long extractTimestampFrom(Variant[] values) {
        if (values.length == 0 || values.length > 2) {
            throw new IllegalArgumentException("Combiner expects args like (timestamp) or (timestamp, offset)");
        }

        return values[0] != null && values[0].getType() == Type.LONG
                ? (Long) values[0].getValue()
                : null;
    }

    private long extractOffsetFrom(Variant[] values) {
        return values.length == 2 && values[1] != null && values[1].getType() == Type.LONG
                ? (long) values[1].getValue()
                : 0L;
    }
}
