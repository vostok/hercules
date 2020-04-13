package ru.kontur.vostok.hercules.elastic.adapter.index;

import ru.kontur.vostok.hercules.protocol.TinyString;
import ru.kontur.vostok.hercules.protocol.Variant;

import java.util.Map;

/**
 * @author Gregory Koshelev
 */
public class IndexMeta {
    private final Map<TinyString, Variant> properties;
    private final String stream;

    public IndexMeta(Map<TinyString, Variant> properties, String stream) {
        this.properties = properties;
        this.stream = stream;
    }

    public Map<TinyString, Variant> getProperties() {
        return properties;
    }

    public String getStream() {
        return stream;
    }
}
