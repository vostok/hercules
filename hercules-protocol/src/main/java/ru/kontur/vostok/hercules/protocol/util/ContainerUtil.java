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

    public static <T> Optional<T> extractOptional(Container container, FieldDescription field) {
        return VariantUtil.extractRegardingType(container.get(field.getName()), field.getType());
    }

    public static <T> T extractRequired(Container container, FieldDescription field) {
        return ContainerUtil.<T>extractOptional(container, field)
                .orElseThrow(() -> new IllegalArgumentException(String.format("Missing required field '%s' of type %s", field.getName(), field.getType())));
    }
}
