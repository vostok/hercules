package ru.kontur.vostok.hercules.protocol.encoder;

import ru.kontur.vostok.hercules.protocol.TimelineSliceState;

/**
 * Writer for Slice state
 *
 * @author Gregory Koshelev
 */
public class TimelineSliceStateWriter implements Writer<TimelineSliceState> {
    /**
     * Write Slice state
     * <code>
     * |...Timeline Slice State...|                                                 <br>
     * |.Slice.|.Offset.|.EventId.|                                                 <br>
     * </code>
     * Where, Slice is Integer, Offset is Long, EventId is 24-byte event id.        <br>
     * EventId consists of Event Timestamp and Event Uuid.
     *
     * @param encoder is Encoder
     * @param value is Slice state to be encoded
     */
    @Override
    public void write(Encoder encoder, TimelineSliceState value) {
        encoder.writeInteger(value.getSlice());
        encoder.writeLong(value.getTtOffset());
        encoder.writeRawBytes(value.getEventId());
    }
}
