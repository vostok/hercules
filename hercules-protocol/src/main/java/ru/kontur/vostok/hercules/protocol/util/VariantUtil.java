package ru.kontur.vostok.hercules.protocol.util;

import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Vector;
import ru.kontur.vostok.hercules.protocol.Variant;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class VariantUtil {

    private VariantUtil() {
    }

    public static Optional<Object> extract(Variant variant) {
        switch (variant.getType()) {
            case BYTE:
            case SHORT:
            case INTEGER:
            case LONG:
            case FLAG:
            case FLOAT:
            case DOUBLE:
            case UUID:
                return Optional.of(variant.getValue());
            case STRING:
                return Optional.of(new String((byte[]) variant.getValue(), StandardCharsets.UTF_8));
            case CONTAINER:
                Map<String, Object> map = new HashMap<>();
                for(Map.Entry<String, Variant> entry : (Container)variant.getValue()) {
                    map.put(entry.getKey(), extract(entry.getValue()).orElse(null));
                }
                return Optional.of(map);
            case VECTOR:
                List<Object> resultList = new ArrayList<>();
                Vector vector = (Vector)variant.getValue();
                Object[] objects = (Object[])vector.getValue();
                for(Object object : objects) {
                    extract(new Variant(vector.getType(), object)).ifPresent(resultList::add);
                }
                return Optional.of(resultList);
            default:
                return Optional.empty();
        }
    }
}
