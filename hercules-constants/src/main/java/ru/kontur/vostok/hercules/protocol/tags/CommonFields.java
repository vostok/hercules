package ru.kontur.vostok.hercules.protocol.tags;

import ru.kontur.vostok.hercules.protocol.util.FieldDescription;
import ru.kontur.vostok.hercules.protocol.Type;

/**
 * CommonFields collection of common fields
 *
 * @author Kirill Sulim
 */
public final class CommonFields {

    public static final FieldDescription ENVIRONMENT_FIELD = FieldDescription.create("env", Type.STRING);

    private CommonFields() {
    }
}
