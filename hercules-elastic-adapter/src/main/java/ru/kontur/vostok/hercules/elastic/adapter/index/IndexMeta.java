package ru.kontur.vostok.hercules.elastic.adapter.index;

import ru.kontur.vostok.hercules.elastic.adapter.format.mapping.Mapping;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.protocol.hpath.HPath;

import java.util.Map;

/**
 * @author Gregory Koshelev
 */
public class IndexMeta {
    private final String stream;
    private final Map<HPath, Variant> properties;
    private final HPath indexPath;
    private final TimestampFormat timestampFormat;
    private final Mapping mapping;

    public IndexMeta(String stream, Map<HPath, Variant> properties, HPath indexPath, TimestampFormat timestampFormat, Mapping mapping) {
        this.stream = stream;
        this.properties = properties;
        this.indexPath = indexPath;
        this.timestampFormat = timestampFormat;
        this.mapping = mapping;
    }

    public String getStream() {
        return stream;
    }

    public Map<HPath, Variant> getProperties() {
        return properties;
    }

    public HPath getIndexPath() {
        return indexPath;
    }

    public TimestampFormat getTimestampFormat() {
        return timestampFormat;
    }

    public Mapping getMapping() {
        return mapping;
    }
}
