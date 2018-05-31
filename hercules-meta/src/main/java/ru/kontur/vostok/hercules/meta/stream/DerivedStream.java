package ru.kontur.vostok.hercules.meta.stream;

/**
 * @author Gregory Koshelev
 */
public class DerivedStream extends Stream {
    private String[] streams;
    private String[] filters;//TODO: Replace with prepared filters

    public String[] getStreams() {
        return streams;
    }

    public void setStreams(String[] streams) {
        this.streams = streams;
    }

    public String[] getFilters() {
        return filters;
    }

    public void setFilters(String[] filters) {
        this.filters = filters;
    }
}
