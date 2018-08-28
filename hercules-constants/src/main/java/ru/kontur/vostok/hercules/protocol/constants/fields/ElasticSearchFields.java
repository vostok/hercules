package ru.kontur.vostok.hercules.protocol.constants.fields;

import ru.kontur.vostok.hercules.protocol.Type;
import ru.kontur.vostok.hercules.protocol.util.FieldDescription;

/**
 * ElasticSearchFields collection of fields for elasticsearch related data
 *
 * @author Kirill Sulim
 */
public final class ElasticSearchFields {

    /**
     * Special name for marking use of special index in elasticsearch
     */
    public static final FieldDescription INDEX_FIELD = FieldDescription.create("$index", Type.STRING);
}
