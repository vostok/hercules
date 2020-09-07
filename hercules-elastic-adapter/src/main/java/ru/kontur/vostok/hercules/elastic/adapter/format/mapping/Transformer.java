package ru.kontur.vostok.hercules.elastic.adapter.format.mapping;

import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.util.ClassUtil;

/**
 * Transforms the field value of the JSON-document to the {@link Variant}.
 *
 * @author Gregory Koshelev
 */
public interface Transformer {
    /**
     * The instance of the {@link PlainTransformer}
     */
    Transformer PLAIN = new PlainTransformer();

    /**
     * Transform the field value of the JSON-document to the variant.
     *
     * @param value the field value
     * @return the variant
     */
    Variant transform(Object value);

    /**
     * Create the transformer from the class name.
     * <p>
     * The class should implement {@link Transformer} interface.
     *
     * @param className the class name
     * @return the transformer instance
     */
    static Transformer fromClass(String className) {
        return ClassUtil.fromClass(className, Transformer.class);
    }
}
