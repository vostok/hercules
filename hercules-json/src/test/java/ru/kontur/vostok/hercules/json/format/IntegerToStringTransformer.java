package ru.kontur.vostok.hercules.json.format;

import ru.kontur.vostok.hercules.json.format.transformer.Transformer;
import ru.kontur.vostok.hercules.protocol.Type;
import ru.kontur.vostok.hercules.protocol.Variant;

/**
 * @author Gregory Koshelev
 */
public class IntegerToStringTransformer implements Transformer {
    @Override
    public Object transform(Variant value) {
        if (value.getType() != Type.INTEGER) {
            throw new IllegalArgumentException("Expect type " + Type.INTEGER + ", but got " + value.getType());
        }
        return value.getValue().toString();
    }
}
