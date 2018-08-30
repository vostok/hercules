package ru.kontur.vostok.hercules.protocol.util;

import ru.kontur.vostok.hercules.protocol.Container;

import java.util.Optional;

/**
 * ContainerUtil
 *
 * @author Kirill Sulim
 */
public class ContainerUtil {

    public static <T> Optional<T> extractOptional(Container container, TagDescription tag) {
        return VariantUtil.extractRegardingType(container.get(tag.getName()), tag.getType());
    }

    public static <T> T extractRequired(Container container, TagDescription tag) {
        return ContainerUtil.<T>extractOptional(container, tag)
                .orElseThrow(() -> new IllegalArgumentException(String.format("Missing required tag '%s' of type %s", tag.getName(), tag.getType())));
    }
}
