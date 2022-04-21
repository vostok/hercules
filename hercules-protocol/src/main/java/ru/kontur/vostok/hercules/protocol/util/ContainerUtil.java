package ru.kontur.vostok.hercules.protocol.util;

import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Type;
import ru.kontur.vostok.hercules.protocol.Variant;

import java.util.Optional;
import java.util.function.Function;

/**
 * ContainerUtil
 *
 * @author Kirill Sulim
 */
public class ContainerUtil {

    public static <T> T extract(Container container, TagDescription<T> tag) {
        Variant variant = container.get(tag.getName());
        Type type = Optional.ofNullable(variant).map(Variant::getType).orElse(null);
        Function<Object, ? extends T> extractor = tag.getExtractors().get(type);
        if (extractor == null) {
            extractor = tag.getExtractors().get(null);
        }
        if (extractor == null) {
            throw new IllegalArgumentException(
                    String.format("Tag '%s' cannot contain value of type '%s' or empty value", tag.getName(), type));
        }
        Object value = Optional.ofNullable(variant).map(Variant::getValue).orElse(null);
        return extractor.apply(value);
    }

    public static Optional<Container> extractContainer(Variant variant) {
        if (variant.getType() == Type.CONTAINER) {
            return Optional.of((Container) variant.getValue());
        }
        return Optional.empty();
    }

    private ContainerUtil() {
        /* static class */
    }
}
