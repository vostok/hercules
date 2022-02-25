package ru.kontur.vostok.hercules.json.format.combiner;

import org.jetbrains.annotations.Nullable;
import ru.kontur.vostok.hercules.protocol.Type;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.util.time.TimeUtil;

/**
 * Combines begin and end timestamp (in 100ns ticks)
 * into the latency is formatted to a string with the measurement unit.
 *
 * @author Gregory Koshelev
 */
public class LatencyStringCombiner implements Combiner {
    /**
     * Combine begin and end timestamp into the latency is formatted to a string with the measurement unit.
     *
     * @param values begin and end timestamp
     * @return the string representation of the latency if both timestamps are present, otherwise {@code null}
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

        return TimeUtil.ticksToPrettyString(endTimestamp - beginTimestamp, false);
    }

}
