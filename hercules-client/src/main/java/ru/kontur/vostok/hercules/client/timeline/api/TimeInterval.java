package ru.kontur.vostok.hercules.client.timeline.api;

/**
 * TimeInterval - describe time interval
 *
 * @author Kirill Sulim
 */
public class TimeInterval {

    private final long from;
    private final long to;

    /**
     * @param from left inclusive time bound
     * @param to right exclusive time bound
     */
    public TimeInterval(long from, long to) {
        this.from = from;
        this.to = to;
    }

    public long getFrom() {
        return from;
    }

    public long getTo() {
        return to;
    }
}
