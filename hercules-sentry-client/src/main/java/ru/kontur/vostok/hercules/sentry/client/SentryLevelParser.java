package ru.kontur.vostok.hercules.sentry.client;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.kontur.vostok.hercules.util.parameter.parsing.Parser;
import ru.kontur.vostok.hercules.util.parameter.parsing.Parsers;
import ru.kontur.vostok.hercules.util.parameter.parsing.ParsingResult;

/**
 * Allows to convert a String value of the level tag of a Hercules event to a Sentry event level.
 * C-sharp client use "warn" as level value, so we must adapt it to sentry Level enum
 * 
 * @author Kirill Sulim
 */

public final class SentryLevelParser implements Parser<SentryLevel> {
    public static final Parser<SentryLevel> PARSER = Parsers.forEnum(SentryLevel.class);

    public @NotNull ParsingResult<SentryLevel> parse(@Nullable String value) {
        return PARSER.parse(prepareLevel(value));
    }

    private String prepareLevel(String original) {
        if ("warn".equalsIgnoreCase(original)) {
            return "WARNING";
        } else {
            // TODO: 22.07.2022 do not use toUpperCase 
            return original.toUpperCase();
        }
    }
}
