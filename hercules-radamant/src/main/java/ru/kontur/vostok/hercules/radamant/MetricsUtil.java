package ru.kontur.vostok.hercules.radamant;


import static ru.kontur.vostok.hercules.tags.MetricsTags.TAGS_VECTOR_TAG;

import java.nio.charset.StandardCharsets;
import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.Type;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.protocol.Vector;
import ru.kontur.vostok.hercules.tags.MetricsTags;

/**
 * @author Tatyana Tokmyanina
 */
public class MetricsUtil {
    public static boolean isFlatMetric(Event event) {
        Variant tags = event.getPayload().tags().get(TAGS_VECTOR_TAG.getName());
        if (tags.getType() != Type.VECTOR) {
            return false;
        }

        Object values = ((Vector) tags.getValue()).getValue();
        if (!(values instanceof Container[])) {
            return false;
        }

        Container[] innerTags = (Container[]) values;
        if (innerTags.length != 1) {
            return false;
        }

        String value = new String(
                (byte[]) innerTags[0].tags().get(MetricsTags.TAG_KEY_TAG.getName()).getValue(),
                StandardCharsets.UTF_8);
        return value.equals("_name");
    }

    private MetricsUtil() {
        /* static class */
    }
}
