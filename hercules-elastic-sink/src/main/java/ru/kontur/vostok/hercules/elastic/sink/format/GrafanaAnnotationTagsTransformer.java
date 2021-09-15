package ru.kontur.vostok.hercules.elastic.sink.format;

import ru.kontur.vostok.hercules.json.format.transformer.Transformer;
import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.TinyString;
import ru.kontur.vostok.hercules.protocol.Type;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.protocol.Vector;
import ru.kontur.vostok.hercules.util.metrics.GraphiteSanitizer;

import java.nio.charset.StandardCharsets;

/**
 * Transforms annotation tags to the Grafana compatible form.
 *
 * @author Gregory Koshelev
 */
public class GrafanaAnnotationTagsTransformer implements Transformer {
    private static final TinyString KEY = TinyString.of("key");
    private static final TinyString VALUE = TinyString.of("value");

    @Override
    public Object transform(Variant value) {
        if (value.getType() != Type.VECTOR) {
            return null;
        }
        Vector vector = (Vector) value.getValue();
        if (vector.getType() != Type.CONTAINER) {
            return null;
        }

        boolean isProjectTagAdded = false;
        boolean isSubprojectTagAdded = false;
        StringBuilder sb = new StringBuilder();
        for (Container container : (Container[]) vector.getValue()) {
            Variant k = container.get(KEY);
            Variant v = container.get(VALUE);
            if (k == null || k.getType() != Type.STRING || v == null || v.getType() != Type.STRING) {
                continue;
            }

            String tagKey = new String((byte[]) k.getValue(), StandardCharsets.UTF_8);
            String tagValue = new String((byte[]) v.getValue(), StandardCharsets.UTF_8);
            if (tagKey.equals("project")) {
                isProjectTagAdded = true;
            }
            if (tagKey.equals("subproject")) {
                isSubprojectTagAdded = true;
            }

            sb.append(GraphiteSanitizer.METRIC_NAME_SANITIZER.sanitize(tagKey));
            sb.append('=');
            sb.append(GraphiteSanitizer.METRIC_NAME_SANITIZER.sanitize(tagValue));
            sb.append(',');
        }

        //FIXME delete subproject tag adding when found a better way to work with optional tags in Grafana
        if (isProjectTagAdded && !isSubprojectTagAdded) {
            sb.append("subproject=null");
        }

        return sb.toString();
    }
}
