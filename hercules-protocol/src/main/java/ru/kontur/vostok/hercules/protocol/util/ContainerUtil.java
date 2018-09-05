package ru.kontur.vostok.hercules.protocol.util;

import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Type;
import ru.kontur.vostok.hercules.protocol.Variant;

import java.util.Objects;
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
        if (Objects.isNull(extractor)) {
            throw new IllegalArgumentException(String.format("Tag '%s' cannot contain value of type '%s'", tag.getName(),  type));
        }
        else {
            Object value = Optional.ofNullable(variant).map(Variant::getValue).orElse(null);
            return extractor.apply(value);
        }
    }
}
