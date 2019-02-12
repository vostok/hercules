package ru.kontur.vostok.hercules.util;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * CollectionUtil
 *
 * @author Kirill Sulim
 */
public final class CollectionUtil {
    public static <T> List<T> nullAsEmpty(List<T> list) {
        if (Objects.nonNull(list)) {
            return list;
        } else {
            return Collections.emptyList();
        }
    }

    public static <T> boolean isNullOrEmpty(Collection<T> collection) {
        return collection == null || collection.isEmpty();
    }

    private CollectionUtil() {
        /* static class */
    }
}
