package ru.kontur.vostok.hercules.json.format.combiner;

import org.jetbrains.annotations.Nullable;
import ru.kontur.vostok.hercules.protocol.Type;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.util.time.TimeUtil;

/**
 * Combines begin and end timestamp (in 100ns ticks) into the latency in milliseconds.
 *
 * @author Gregory Koshelev
 */
public class LatencyMsCombiner implements Combiner {
    /**
     * Combine begin and end timestamp into the latency in milliseconds.
     *
     * @param values begin and end timestamp in 100ns ticks
     * @return the latency in milliseconds if both timestamps are present, otherwise {@code null}
     */
    @Override
    public @Nullable Object combine(Variant... values) {
        if (values.length != 2) {
            throw new IllegalArgumentException("Combiner expects 2 timestamps");
        }
        if (values[0] == null || values[1] == null
                || values[0].getType() != Type.LONG || values[1].getType() != Type.LONG) {
            return null;
        }

        long beginTimestamp = (long) values[0].getValue();
        long endTimestamp = (long) values[1].getValue();

        return TimeUtil.ticksToMillis(endTimestamp - beginTimestamp);
    }
}
