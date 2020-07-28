package ru.kontur.vostok.hercules.json.format.transformer;

import ru.kontur.vostok.hercules.protocol.Type;
import ru.kontur.vostok.hercules.protocol.Variant;

import java.nio.charset.StandardCharsets;

/**
 * Transforms the value of any scalar type (all types except {@link Type#CONTAINER} and {@link Type#VECTOR}) to a string.
 *
 * @author Gregory Koshelev
 */
public class ScalarToStringTransformer implements Transformer {
    /**
     * Transform the value of any scalar type (all types except {@link Type#CONTAINER} and {@link Type#VECTOR}) to a string.
     *
     * @param value the value
     * @return the transformed result or {@code null} if the value cannot be transformed
     */
    @Override
    public Object transform(Variant value) {
        switch (value.getType()) {
            case TYPE:
            case BYTE:
            case SHORT:
            case INTEGER:
            case LONG:
            case FLAG:
            case FLOAT:
            case DOUBLE:
            case UUID:
                return value.getValue().toString();
            case NULL:
                return null;
            case STRING:
                return new String((byte[]) value.getValue(), StandardCharsets.UTF_8);
            default:
                return null;
        }
    }
}
