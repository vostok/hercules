package ru.kontur.vostok.hercules.protocol;

/**
 * Timeline state for slice.                                                         <br>
 * TimelineSliceState is used to identify the last read position of Timeline.
 *
 * @author Gregory Koshelev
 */
public class TimelineSliceState {
    /**
     * Slice id
     */
    private final int slice;
    /**
     * Timetrap offset is measured in Unix timestamp in millis
     */
    private final long ttOffset;
    /**
     * Event Id is 24-byte value which consists of event timestamp and event uuid
     */
    private final byte[] eventId;

    /**
     * Create immutable Timeline state for slice
     *
     * @param slice is slice id
     * @param ttOffset is Timetrap offset, Unix timestamp in millis
     * @param eventId is 24-byte event id
     */
    public TimelineSliceState(int slice, long ttOffset, byte[] eventId) {
        this.slice = slice;
        this.ttOffset = ttOffset;
        this.eventId = eventId;
    }

    public int getSlice() {
        return slice;
    }

    public long getTtOffset() {
        return ttOffset;
    }

    public byte[] getEventId() {
        return eventId;
    }
}
