package ru.kontur.vostok.hercules.protocol.fields;

import ru.kontur.vostok.hercules.protocol.Type;
import ru.kontur.vostok.hercules.protocol.util.FieldDescription;

/**
 * MetricsFields collection of metrics fields
 *
 * @author Kirill Sulim
 */
public class MetricsFields {
    public static final FieldDescription METRIC_NAME_FIELD = FieldDescription.create("metric-name", Type.TEXT);
    public static final FieldDescription METRIC_VALUE_FIELD = FieldDescription.create("metric-value", Type.DOUBLE);
}
