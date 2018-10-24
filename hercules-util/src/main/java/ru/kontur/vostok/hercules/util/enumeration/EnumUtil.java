package ru.kontur.vostok.hercules.util.enumeration;

import java.util.Optional;

/**
 * EnumUtil
 *
 * @author Kirill Sulim
 */
public final class EnumUtil {

    /**
     * @deprecated Use {@link ru.kontur.vostok.hercules.util.parsing.Parsers#enumParser(Class)}
     */
    @Deprecated
    public static <T extends Enum<T>> Optional<T> parseOptional(Class<T> clazz, String s) {
        try {
            return Optional.of(parse(clazz, s));
        }
        catch (NullPointerException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    /**
     * @deprecated Use {@link ru.kontur.vostok.hercules.util.parsing.Parsers#enumParser(Class)}
     */
    @Deprecated
    public static <T extends Enum<T>> T parse(Class<T> clazz, String s) {
        return Enum.valueOf(clazz, s.toUpperCase());
    }
}
