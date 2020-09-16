package ru.kontur.vostok.hercules.protocol;

/**
 * Timeline state for slice.                                                         <br>
 * TimelineSliceState is used to identify the last read position of Timeline.
 *
 * @author Gregory Koshelev
 */
public class TimelineSliceState {
    private static final int SIZE_OF_SLICE = 4;
    private static final int SIZE_OF_TT_OFFSET = 8;
    private static final int SIZE_OF_EVENT_ID = 24;

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
        if (eventId.length != SIZE_OF_EVENT_ID) {
            throw new IllegalArgumentException("Event id should have size of " + SIZE_OF_EVENT_ID + " bytes");
        }

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

    public int sizeOf() {
        return fixedSizeOf();
    }

    public static int fixedSizeOf() {
        return SIZE_OF_SLICE + SIZE_OF_TT_OFFSET + SIZE_OF_EVENT_ID;
    }
}
