package ru.kontur.vostok.hercules.elastic.adapter.parser;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * @author Daniil Zhenikhov
 */
public class ApiKeys {
    @JsonProperty("elk_api_keys")
    private Map<String, List<String>> values;

    public Map<String, List<String>> getValues() {
        return values;
    }

    public void setValues(Map<String, List<String>> values) {
        this.values = values;
    }
}
