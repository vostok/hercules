package ru.kontur.vostok.hercules.gate.client;

/**
 * @author tsypaev
 */
public class GreyListTopologyElement {

    private String url;
    private long entryTime;

    public GreyListTopologyElement(String url) {
        this.url = url;
        this.entryTime = System.currentTimeMillis();
    }

    public String getUrl() {
        return url;
    }

    public long getEntryTime() {
        return entryTime;
    }


}
