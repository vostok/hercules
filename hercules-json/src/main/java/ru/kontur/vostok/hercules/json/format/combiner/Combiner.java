package ru.kontur.vostok.hercules.json.format.combiner;

import org.jetbrains.annotations.Nullable;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.util.ClassUtil;

/**
 * Combines two or more values into single one.
 *
 * @author Gregory Koshelev
 */
@FunctionalInterface
public interface Combiner {
    /**
     * Combine values into single one.
     * Each value in {@code values} can be {@code null}.
     *
     * @param values values to combine
     * @return the result
     */
    @Nullable Object combine(Variant... values);

    static Combiner fromClass(String className) {
        return ClassUtil.fromClass(className, Combiner.class);
    }
}
