package ru.kontur.vostok.hercules.protocol.util;

import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Type;

import java.util.Optional;

/**
 * ContainerUtil
 *
 * @author Kirill Sulim
 */
public class ContainerUtil {

    public static <T> Optional<T> extractOptional(Container container, String fieldName, Type type, Type... types) {
        return VariantUtil.extractRegardingType(container.get(fieldName), type, types);
    }

    public static <T> T extractRequired(Container container, String fieldName, Type type, Type ... types) {
        return ContainerUtil.<T>extractOptional(container, fieldName, type, types)
                .orElseThrow(() -> new IllegalArgumentException(String.format("Missing required field '%s' of type %s", fieldName, type)));
    }
}
