package ru.kontur.vostok.hercules.protocol.util;

import org.jetbrains.annotations.Nullable;
import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Type;
import ru.kontur.vostok.hercules.protocol.Variant;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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
        } else {
            Object value = Optional.ofNullable(variant).map(Variant::getValue).orElse(null);
            return extractor.apply(value);
        }
    }

    /**
     * Converts Container to Object Map
     *
     * @param container Container for converting
     * @return result Object Map
     */
    public static Map<String, Object> toObjectMap(final Container container) {
        return toObjectMap(container, null);
    }

    /**
     * Converts Container to Object Map
     *
     * @param container Container for converting
     * @param exclusionSet Set of string keys which should not be put to result Map
     * @return result Object Map
     */
    public static Map<String, Object> toObjectMap(final Container container, @Nullable Set<String> exclusionSet) {
        if(exclusionSet == null) {
            exclusionSet = Collections.emptySet();
        }
        Map<String, Object> map = new HashMap<>();
        for (Map.Entry<String, Variant> entry : container) {
            String key = entry.getKey();
            if (!exclusionSet.contains(key)) {
                Optional<Object> valueOptional = VariantUtil.extract(entry.getValue());
                map.put(key, valueOptional.orElse(null));
            }
        }
        return map;
    }

    private ContainerUtil() {
        /* static class */
    }
}
