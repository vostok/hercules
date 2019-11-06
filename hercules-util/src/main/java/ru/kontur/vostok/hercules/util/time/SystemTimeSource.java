package ru.kontur.vostok.hercules.util.time;

/**
 * System time source uses the system clock.
 *
 * @author Gregory Koshelev
 */
public class SystemTimeSource implements TimeSource {
    @Override
    public long milliseconds() {
        return System.currentTimeMillis();
    }

    @Override
    public long nanoseconds() {
        return System.nanoTime();
    }

    @Override
    public void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            // Thread was woken up early
            Thread.currentThread().interrupt();
        }
    }
}
