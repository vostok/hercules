package ru.kontur.vostok.hercules.elastic.adapter.index.config;

import ru.kontur.vostok.hercules.elastic.adapter.index.TimestampFormat;

import java.util.Map;

/**
 * @author Gregory Koshelev
 */
public class ConfigIndexMeta {
    private String stream;
    private Map<String, String> properties;
    private String indexPath;
    private TimestampFormat timestampFormat;
    private String mappingFile;

    public String getStream() {
        return stream;
    }

    public void setStream(String stream) {
        this.stream = stream;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }

    public String getIndexPath() {
        return indexPath;
    }

    public void setIndexPath(String indexPath) {
        this.indexPath = indexPath;
    }

    public TimestampFormat getTimestampFormat() {
        return timestampFormat;
    }

    public void setTimestampFormat(TimestampFormat timestampFormat) {
        this.timestampFormat = timestampFormat;
    }

    public String getMappingFile() {
        return mappingFile;
    }

    public void setMappingFile(String mappingFile) {
        this.mappingFile = mappingFile;
    }
}
