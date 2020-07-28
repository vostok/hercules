package ru.kontur.vostok.hercules.json.format.combiner;

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
     *
     * @param values values to combine
     * @return the result
     */
    Object combine(Variant... values);

    static Combiner fromClass(String className) {
        return ClassUtil.fromClass(className, Combiner.class);
    }
}
