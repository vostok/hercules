package ru.kontur.vostok.hercules.splitter.models;

import java.util.Objects;

/**
 * Record that stores event data with destination node name.
 *
 * @author Aleksandr Yuferov
 */
public class Envelope {
    private final byte[] data;
    private final String destination;

    /**
     * Constructor.
     *
     * @param data        Event data.
     * @param destination Destination node name.
     */
    public Envelope(byte[] data, String destination) {
        this.data = Objects.requireNonNull(data, "data");
        this.destination = Objects.requireNonNull(destination, "destination");
    }

    /**
     * Getter for event data.
     *
     * @return Event data.
     */
    public byte[] data() {
        return data;
    }

    /**
     * Getter for destination node name.
     *
     * @return Destination node name.
     */
    public String destination() {
        return destination;
    }
}
