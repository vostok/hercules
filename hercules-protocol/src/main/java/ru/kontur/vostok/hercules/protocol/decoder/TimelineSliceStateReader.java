package ru.kontur.vostok.hercules.protocol.decoder;

import ru.kontur.vostok.hercules.protocol.TimelineSliceState;

/**
 * Reader for Slice state
 *
 * @author Gregory Koshelev
 */
public class TimelineSliceStateReader implements Reader<TimelineSliceState> {
    /**
     * Read Slice state.
     * <p>
     * <code>
     * |...Timeline Slice State...|                                                 <br>
     * |.Slice.|.Offset.|.EventId.|                                                 <br>
     * </code>
     * Where, Slice is Integer, Offset is Long, EventId is 24-byte event id.        <br>
     * EventId consists of Event Timestamp and Event Uuid.
     *
     * @param decoder is Decoder
     * @return Slice state was read
     */
    @Override
    public TimelineSliceState read(Decoder decoder) {
        return new TimelineSliceState(
                decoder.readInteger(),
                decoder.readLong(),
                decoder.readBytes(24));
    }
}
