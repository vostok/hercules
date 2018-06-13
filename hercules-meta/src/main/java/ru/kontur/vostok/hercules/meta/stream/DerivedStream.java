package ru.kontur.vostok.hercules.meta.stream;

import ru.kontur.vostok.hercules.meta.filter.Filter;

/**
 * @author Gregory Koshelev
 */
public class DerivedStream extends Stream {
    private String[] streams;
    private Filter[] filters;

    public String[] getStreams() {
        return streams;
    }

    public void setStreams(String[] streams) {
        this.streams = streams;
    }

    public Filter[] getFilters() {
        return filters;
    }

    public void setFilters(Filter[] filters) {
        this.filters = filters;
    }
}
