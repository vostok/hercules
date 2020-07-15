package ru.kontur.vostok.hercules.json.transformer;

import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.TinyString;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.protocol.Vector;
import ru.kontur.vostok.hercules.util.Maps;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Gregory Koshelev
 */
public class PlainTransformer implements Transformer {
    @Override
    public Object transform(Variant value) {
        return transformVariant(value);
    }

    private static Object transformVariant(Variant value) {
        switch (value.getType()) {
            case CONTAINER:
                return transformContainer((Container) value.getValue());
            case BYTE:
            case SHORT:
            case INTEGER:
            case LONG:
            case FLAG:
            case FLOAT:
            case DOUBLE:
            case UUID:
            case NULL:
                return value.getValue();
            case STRING:
                return new String((byte[]) value.getValue(),StandardCharsets.UTF_8);
            case VECTOR:
                return transformVector((Vector) value.getValue());
            default:
                throw new IllegalArgumentException("Not implemented for type " + value.getType());
        }
    }

    private static Map<String, Object> transformContainer(Container container) {
        Map<String, Object> map = new LinkedHashMap<>(Maps.effectiveHashMapCapacity(container.count()));
        for (Map.Entry<TinyString, Variant> tag : container.tags().entrySet()) {
            map.put(tag.getKey().toString(), transformVariant(tag.getValue()));
        }
        return map;
    }

    private static Object transformVector(Vector vector) {
        switch (vector.getType()) {
            case CONTAINER:
                return transformContainers((Container[]) vector.getValue());
            case BYTE:
            case SHORT:
            case INTEGER:
            case LONG:
            case FLAG:
            case FLOAT:
            case DOUBLE:
            case UUID:
            case NULL:
                return vector.getValue();
            case STRING:
                return transformStrings((byte[][]) vector.getValue());
            case VECTOR:
                return transformVectors((Vector[]) vector.getValue());
            default:
                throw new IllegalArgumentException("Not implemented for vector of type " + vector.getType());
        }
    }

    private static Object transformContainers(Container[] containers) {
        List<Map<String, Object>> maps = new ArrayList<>(containers.length);
        for (Container container : containers) {
            maps.add(transformContainer(container));
        }
        return maps;
    }

    private static Object transformStrings(byte[][] bytes) {
        String[] strings = new String[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            strings[i] = new String(bytes[i], StandardCharsets.UTF_8);
        }
        return strings;
    }

    private static Object transformVectors(Vector[] vectors) {
        Object[] objects = new Object[vectors.length];
        for (int i = 0; i < vectors.length; i++) {
            objects[i] = transformVector(vectors[i]);
        }
        return objects;
    }
}
