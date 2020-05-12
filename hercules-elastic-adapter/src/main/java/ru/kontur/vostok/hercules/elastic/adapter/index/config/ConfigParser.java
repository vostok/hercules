package ru.kontur.vostok.hercules.elastic.adapter.index.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.type.MapType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import ru.kontur.vostok.hercules.elastic.adapter.index.IndexMeta;
import ru.kontur.vostok.hercules.protocol.TinyString;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.util.Maps;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * PoC solution to configure IndexManager
 *
 * @author Gregory Koshelev
 */
public class ConfigParser {
    private static final ObjectReader OBJECT_READER;

    static {
        ObjectMapper mapper = new ObjectMapper().
                configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        TypeFactory typeFactory = mapper.getTypeFactory();
        MapType mapType = typeFactory.constructMapType(HashMap.class, String.class, ConfigIndexMeta.class);
        OBJECT_READER = mapper.readerFor(mapType);
    }

    public static Map<String, IndexMeta> parse(InputStream in) {
        Map<String, ConfigIndexMeta> map;
        try {
            map = OBJECT_READER.readValue(in);
        } catch (IOException ex) {
            throw new IllegalArgumentException(ex);
        }
        Map<String, IndexMeta> result = new HashMap<>(Maps.effectiveHashMapCapacity(map.size()));
        map.forEach((index, meta) -> result.put(index, from(meta)));
        return result;
    }

    private static IndexMeta from(ConfigIndexMeta src) {
        if (src.getProperties() == null) {
            return new IndexMeta(Collections.emptyMap(), src.getStream());
        }

        Map<TinyString, Variant> properties = new HashMap<>(Maps.effectiveHashMapCapacity(src.getProperties().size()));
        src.getProperties().forEach((k, v) -> properties.put(TinyString.of(k), Variant.ofString(v)));
        return new IndexMeta(properties, src.getStream());
    }
}
