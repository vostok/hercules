package ru.kontur.vostok.hercules.sentry.sink.converters;

import io.sentry.event.Event;
import ru.kontur.vostok.hercules.util.enumeration.EnumUtil;
import ru.kontur.vostok.hercules.util.functional.Result;

import java.util.Optional;

/**
 * Allows to convert a String value of the level tag of a Hercules event to a Sentry event level
 *
 * @author Kirill Sulim
 */
public final class SentryLevelEnumParser {

    public static Optional<Event.Level> parse(String value) {
        value = prepareLevel(value);
        return EnumUtil.parseOptional(Event.Level.class, value);
    }

    public static Result<Event.Level, String> parseAsResult(String value) {
        return parse(value)
                .map(Result::<Event.Level, String>ok)
                .orElseGet(() -> Result.error(String.format("Cannot parse '%s' as event level", value)));
    }

    /*
     * C-sharp client use "warn" as level value, so we must adapt it to sentry Level enum
     */
    private static String prepareLevel(String original) {
        if ("warn".equals(original.toLowerCase())) {
            return "warning";
        } else {
            return original;
        }
    }
}
