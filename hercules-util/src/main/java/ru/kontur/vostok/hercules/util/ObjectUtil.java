package ru.kontur.vostok.hercules.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

/**
 * ObjectUtil - collection of object util functions
 *
 * @author Kirill Sulim
 */
public final class ObjectUtil {

    public static final UUID NIL_UUID = new UUID(0, 0);

    public static <T> T firstNonNull(T ... values) {
        for (T value : values) {
            if (Objects.nonNull(value)) {
                return value;
            }
        }
        return null;
    }

    /**
     * Convert null to UUID null value
     */
    @NotNull
    public static UUID nullToNilUuidValue(@Nullable final UUID uuid) {
        if (Objects.isNull(uuid)) {
            return NIL_UUID;
        } else {
            return uuid;
        }
    }

    /**
     * Convert null UUID value to null
     */
    @Nullable
    public static UUID nilUuidValueToNull(@NotNull final UUID uuid) {
        if (NIL_UUID.equals(uuid)) {
            return null;
        } else {
            return uuid;
        }
    }


    private ObjectUtil() {
        /* static class */
    }
}
