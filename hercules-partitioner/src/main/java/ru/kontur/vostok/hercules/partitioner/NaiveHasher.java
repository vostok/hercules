package ru.kontur.vostok.hercules.partitioner;

import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.Type;
import ru.kontur.vostok.hercules.protocol.Variant;

/**
 * @author Gregory Koshelev
 */
public class NaiveHasher implements Hasher {
    public static int hash(Type type, Object value) {
        return functions[type.value].hash(value);
    }

    @Override
    public int hash(Event event, String[] tags) {
        int hash = 0;
        for (String tag : tags) {
            Variant tagValue = event.getTags().get(tag);
            hash = 31 * hash + ((tagValue != null) ? hash(tagValue.getType(), tagValue.getValue()) : 0);
        }
        return 0;
    }

    private static HashFunction[] functions = {
            value -> 0,/* RESERVED */
            value -> (Byte) value,/* BYTE */
            value -> (Short) value,/* SHORT */
            value -> (Integer) value,/* INTEGER */
            value -> {/* LONG */
                long v = (Long) value;
                return (int) (v ^ (v >> 32));
            },
            value -> ((Boolean) value) ? 2029 : 2027,/* FLAG */
            value -> Float.floatToIntBits((Float) value),/* FLOAT */
            value -> {/* DOUBLE */
                long result = Double.doubleToLongBits((Double) value);
                return (int) (result ^ (result >> 32));
            },
            NaiveHasher::hashOfByteArray,/* STRING */
            NaiveHasher::hashOfByteArray,/* TEXT */
            value -> 0,/* RESERVED */
            NaiveHasher::hashOfByteArray,/* BYTE_ARRAY */
            value -> 0,/* SHORT_ARRAY */
            value -> 0,/* INTEGER_ARRAY */
            value -> 0,/* LONG_ARRAY */
            value -> 0,/* FLAG_ARRAY */
            value -> 0,/* FLOAT_ARRAY */
            value -> 0,/* FLOAT_ARRAY */
            value -> 0,/* DOUBLE_ARRAY */
            value -> 0,/* STRING_ARRAY */
            value -> 0,/* TEXT_ARRAY */
            value -> 0,/* RESERVED */
            NaiveHasher::hashOfByteArray,/* BYTE_VECTOR */
            value -> 0,/* SHORT_VECTOR */
            value -> 0,/* INTEGER_VECTOR */
            value -> 0,/* LONG_VECTOR */
            value -> 0,/* FLAG_VECTOR */
            value -> 0,/* FLOAT_VECTOR */
            value -> 0,/* FLOAT_VECTOR */
            value -> 0,/* DOUBLE_VECTOR */
            value -> 0,/* STRING_VECTOR */
            value -> 0,/* TEXT_VECTOR */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0,/* RESERVED */
            value -> 0/* RESERVED */
    };

    private interface HashFunction {
        int hash(Object value);
    }

    public static int hashOfByteArray(Object value) {
        byte[] bytes = (byte[]) value;
        int hash = 0;
        for (byte b : bytes) {
            hash = 31 * hash + b;
        }
        return hash;
    }
}
