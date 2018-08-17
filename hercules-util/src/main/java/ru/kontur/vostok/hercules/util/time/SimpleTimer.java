package ru.kontur.vostok.hercules.util.time;

/**
 * SimpleTimer - the simplest timer class for better readability
 *
 * @author Kirill Sulim
 */
public class SimpleTimer {

    private final long startMillis;

    public SimpleTimer() {
        this.startMillis = System.currentTimeMillis();
    }

    public long elapsed() {
        return System.currentTimeMillis() - startMillis;
    }
}
