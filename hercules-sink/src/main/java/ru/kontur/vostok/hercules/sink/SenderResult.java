package ru.kontur.vostok.hercules.sink;

/**
 * Result of Sender's processing of events.
 *
 * @author Gregory Koshelev
 */
public class SenderResult {
    private final boolean success;
    private final int processedEvents;
    private final int rejectedEvents;

    private SenderResult(boolean success, int processedEvents, int rejectedEvents) {
        this.success = success;
        this.processedEvents = processedEvents;
        this.rejectedEvents = rejectedEvents;
    }

    public boolean isSuccess() {
        return success;
    }

    /**
     * Count of successfully processed events.
     *
     * @return count of successfully processed events
     */
    public int getProcessedEvents() {
        return processedEvents;
    }

    /**
     * Count of rejected events.
     *
     * @return cont of rejected events
     */
    public int getRejectedEvents() {
        return rejectedEvents;
    }

    /**
     * Successful processing.
     *
     * @param processedEvents count of successfully processed events
     * @param rejectedEvents  count of rejected events
     * @return successful result
     */
    public static SenderResult ok(int processedEvents, int rejectedEvents) {
        return new SenderResult(true, processedEvents, rejectedEvents);
    }

    /**
     * Failed processing.
     *
     * @return failed result
     */
    public static SenderResult fail() {
        return new SenderResult(false, 0, 0);
    }
}
