package ru.kontur.vostok.hercules.sentry.sink.converters;

import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Type;
import ru.kontur.vostok.hercules.protocol.Variant;

import java.util.HashMap;
import java.util.Map;

public class SentryContextsConverter {

    public static Map<String, Map<String, Object>> convert(Container contexts) {
        Map<String, Map<String, Object>> contextMap = new HashMap<>();
        for (Map.Entry<String, Variant> entry : contexts) {
            if (entry.getValue().getType() != Type.CONTAINER) {
                continue;
            }
            String containerKey = entry.getKey();
            Container container;
            try {
                container = (Container) entry.getValue().getValue();
            } catch (ClassCastException e) {
                continue;
            }
            Map<String, Object> map = SentryToMapConverter.convert(container, null);
            contextMap.put(containerKey, map);
        }
        return contextMap;
    }
}
