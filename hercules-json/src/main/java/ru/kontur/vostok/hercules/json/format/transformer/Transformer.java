package ru.kontur.vostok.hercules.json.format.transformer;

import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.util.ClassUtil;

/**
 * Transforms the value.
 *
 * @author Gregory Koshelev
 */
@FunctionalInterface
public interface Transformer {
    /**
     * The instance of {@link PlainTransformer}.
     */
    Transformer PLAIN = new PlainTransformer();

    /**
     * Transform the value.
     *
     * @param value the value
     * @return the transformed result
     */
    Object transform(Variant value);

    static Transformer fromClass(String className) {
        return ClassUtil.fromClass(className, Transformer.class);
    }
}
