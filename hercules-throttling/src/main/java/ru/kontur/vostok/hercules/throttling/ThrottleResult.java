package ru.kontur.vostok.hercules.throttling;

/**
 * Throttling result
 *
 * @author Gregory Koshelev
 */
public class ThrottleResult {
    private static final ThrottleResult EXPIRED = new ThrottleResult(0, ThrottledBy.EXPIRATION);
    private static final ThrottleResult INTERRUPTED = new ThrottleResult(0, ThrottledBy.INTERRUPTION);

    private final long capacity;
    private final ThrottledBy reason;

    private ThrottleResult(long capacity, ThrottledBy reason) {
        this.capacity = capacity;
        this.reason = reason;
    }

    public ThrottledBy reason() {
        return reason;
    }

    long capacity() {
        return capacity;
    }

    public static ThrottleResult passed(long capacity) {
        return new ThrottleResult(capacity, ThrottledBy.NONE);
    }

    public static ThrottleResult expired() {
        return EXPIRED;
    }

    public static ThrottleResult interrupted() {
        return INTERRUPTED;
    }
}
