package ru.kontur.vostok.hercules.protocol.constants.fields;

import ru.kontur.vostok.hercules.protocol.util.FieldDescription;
import ru.kontur.vostok.hercules.protocol.Type;

/**
 * CommonFields collection of common fields
 *
 * @author Kirill Sulim
 */
public final class CommonFields {

    /**
     * Environment field
     */
    public static final FieldDescription ENVIRONMENT_FIELD = FieldDescription.create("env", Type.STRING);

    /**
     * Project field
     */
    public static final FieldDescription PROJECT_FIELD = FieldDescription.create("proj", Type.STRING);

    private CommonFields() {
    }
}
