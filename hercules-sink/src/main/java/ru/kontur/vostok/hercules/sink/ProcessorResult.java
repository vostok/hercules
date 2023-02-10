package ru.kontur.vostok.hercules.sink;

/**
 * Result of events processing.
 *
 * @author Gregory Koshelev
 */
public class ProcessorResult {
    private final boolean success;
    private final int processedEvents;
    private final int rejectedEvents;

    private ProcessorResult(boolean success, int processedEvents, int rejectedEvents) {
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
    public static ProcessorResult ok(int processedEvents, int rejectedEvents) {
        return new ProcessorResult(true, processedEvents, rejectedEvents);
    }

    /**
     * Failed processing.
     *
     * @return failed result
     */
    public static ProcessorResult fail() {
        return FAIL_RESULT;
    }

    private static final ProcessorResult FAIL_RESULT = new ProcessorResult(false, 0, 0);
}
