package ru.kontur.vostok.hercules.util.uuid;

import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Methods for handling UUID strings
 *
 * @author Petr Demenev
 */
public class UuidUtil {
    private static final Pattern UUID_PATTERN =
            Pattern.compile("[0-9A-Fa-f]{8}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{12}");
    private static final Pattern UUID_WITHOUT_HYPHENS_PATTERN = Pattern.compile("[0-9A-Fa-f]{32}");

    public static boolean isUuid(String string) {
        return UUID_PATTERN.matcher(string).matches();
    }

    public static boolean isUuidWithoutHyphens(String string) {
        return UUID_WITHOUT_HYPHENS_PATTERN.matcher(string).matches();
    }

    public static UUID fromString(String uuidString) {
        if (isUuid(uuidString)) {
            return UUID.fromString(uuidString);
        } else if (isUuidWithoutHyphens(uuidString)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(uuidString, 0, 8).
                    append("-").
                    append(uuidString, 8, 12).
                    append("-").
                    append(uuidString, 12, 16).
                    append("-").
                    append(uuidString, 16, 20).
                    append("-").
                    append(uuidString, 20, 32);
            return UUID.fromString(stringBuilder.toString());
        } else {
            throw new IllegalArgumentException("Invalid UUID string: "+ uuidString);
        }
    }

    private UuidUtil(){
        /* static class */
    }
}
