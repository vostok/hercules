package ru.kontur.vostok.hercules.gate.client;

/**
 * @author tsypaev
 */
public class GreyListTopologyElement {

    private final String url;
    private final long entryTime;

    public GreyListTopologyElement(String url, long entryTimeMs) {
        this.url = url;
        this.entryTime = entryTimeMs;
    }

    public String getUrl() {
        return url;
    }

    public long elapsedFrom(long now) {
        return now - entryTime;
    }
}
