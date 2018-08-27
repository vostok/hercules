package ru.kontur.vostok.hercules.protocol.fields;

import ru.kontur.vostok.hercules.protocol.util.FieldDescription;
import ru.kontur.vostok.hercules.protocol.Type;

/**
 * CommonFields collection of common fields
 *
 * @author Kirill Sulim
 */
public final class CommonFields {

    public static final FieldDescription INDEX_FIELD = FieldDescription.create("$index", Type.STRING);
    public static final FieldDescription ENVIRONMENT_FIELD = FieldDescription.create("env", Type.STRING);
    public static final FieldDescription PROJECT_FIELD = FieldDescription.create("proj", Type.STRING);

    private CommonFields() {
    }
}
