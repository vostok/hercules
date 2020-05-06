package ru.kontur.vostok.hercules.elastic.adapter.index.config;

import java.util.Map;

/**
 * @author Gregory Koshelev
 */
public class ConfigIndexMeta {
    private String stream;
    private Map<String, String> properties;

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
}
