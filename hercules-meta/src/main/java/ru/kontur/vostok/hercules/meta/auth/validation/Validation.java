package ru.kontur.vostok.hercules.meta.auth.validation;

import ru.kontur.vostok.hercules.meta.filter.Filter;

/**
 * @author Gregory Koshelev
 */
public class Validation {
    private String apiKey;
    private String stream;
    private Filter[] filters;

    public Validation() {
    }

    public Validation(String apiKey, String stream, Filter[] filters) {
        this.apiKey = apiKey;
        this.stream = stream;
        this.filters = filters;
    }

    public String getApiKey() {
        return apiKey;
    }
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getStream() {
        return stream;
    }
    public void setStream(String stream) {
        this.stream = stream;
    }

    public Filter[] getFilters() {
        return filters;
    }
    public void setFilters(Filter[] filters) {
        this.filters = filters;
    }
}
