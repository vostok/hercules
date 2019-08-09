package ru.kontur.vostok.hercules.sentry.sink.converters;

import org.jetbrains.annotations.Nullable;
import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.protocol.util.VariantUtil;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class SentryToMapConverter {

    public static Map<String, Object> convert(final Container container, @Nullable Set<String> exclusionSet) {
        if(exclusionSet == null) {
            exclusionSet = Collections.emptySet();
        }
        Map<String, Object> stringMap = new HashMap<>();
        for (Map.Entry<String, Variant> entry : container) {
            String key = entry.getKey();
            if (!exclusionSet.contains(key)) {
                Optional<Object> valueOptional = VariantUtil.extract(entry.getValue());
                stringMap.put(key, valueOptional.orElse(null));
            }
        }
        return stringMap;
    }
}
