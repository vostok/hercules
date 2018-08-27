package ru.kontur.vostok.hercules.protocol.fields;

import ru.kontur.vostok.hercules.protocol.util.FieldDescription;
import ru.kontur.vostok.hercules.protocol.Type;

/**
 * CommonFields collection of common fields
 *
 * @author Kirill Sulim
 */
public final class CommonFields {

    /**
     * Special name for marking use of special index in elasticsearch
     */
    public static final FieldDescription INDEX_FIELD = FieldDescription.create("$index", Type.STRING);

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
