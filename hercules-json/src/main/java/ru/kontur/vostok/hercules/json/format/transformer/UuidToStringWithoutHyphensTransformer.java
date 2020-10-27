package ru.kontur.vostok.hercules.json.format.transformer;

import ru.kontur.vostok.hercules.protocol.Type;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.util.uuid.UuidUtil;

import java.util.UUID;

/**
 * Transforms the value of {@link Type#UUID} type to a string without hyphens.
 *
 * @author Vladimir Tsypaev
 */
public class UuidToStringWithoutHyphensTransformer implements Transformer {

    /**
     * Transforms the value of {@link Type#UUID} type to a string without hyphens.
     *
     * @param value the value
     * @return the transformed result or {@code null} if the value cannot be transformed
     */
    @Override
    public Object transform(Variant value) {
        if (value.getType() != Type.UUID) {
            return null;
        }
        return UuidUtil.getUuidWithoutHyphens((UUID)value.getValue());
    }
}
