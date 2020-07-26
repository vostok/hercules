package ru.kontur.vostok.hercules.json.format.combiner;

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
     * @param values timestamp and timezone offset
     * @return ISO 8601 formatted date and time string
     */
    @Override
    public Object combine(Variant... values) {
        if (values.length != 2) {
            throw new IllegalArgumentException("Combiner expects 2 args, but got " + values.length);
        }
        Variant timestamp = values[0];
        Variant offset = values[1];

        ZonedDateTime dateTime = ZonedDateTime.ofInstant(
                TimeUtil.unixTicksToInstant((Long) timestamp.getValue()),
                ZoneOffset.ofTotalSeconds((int) TimeUtil.ticksToSeconds((Long) offset.getValue())));
        return FORMATTER.format(dateTime);
    }
}
