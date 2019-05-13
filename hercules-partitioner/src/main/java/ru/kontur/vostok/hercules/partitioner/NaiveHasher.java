package ru.kontur.vostok.hercules.partitioner;

import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.Type;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.protocol.Vector;
import ru.kontur.vostok.hercules.protocol.hpath.HPath;

import java.util.Arrays;
import java.util.UUID;

/**
 * @author Gregory Koshelev
 */
public class NaiveHasher implements Hasher {
    private static final HashFunction[] TYPE_HASH_FUNCTIONS = new HashFunction[256];
    private static final HashFunction[] TYPE_OF_VECTOR_HASH_FUNCTIONS = new HashFunction[256];

    static {
        Arrays.setAll(TYPE_HASH_FUNCTIONS, idx -> value -> {
            throw new IllegalArgumentException("Unknown type with code " + idx);
        });

        TYPE_HASH_FUNCTIONS[Type.CONTAINER.code] = value -> 0;
        TYPE_HASH_FUNCTIONS[Type.BYTE.code] = value -> (Byte) value;
        TYPE_HASH_FUNCTIONS[Type.SHORT.code] = value -> (Short) value;
        TYPE_HASH_FUNCTIONS[Type.INTEGER.code] = value -> (Integer) value;
        TYPE_HASH_FUNCTIONS[Type.LONG.code] = value -> hash((Long) value);
        TYPE_HASH_FUNCTIONS[Type.FLAG.code] = value -> ((Boolean) value) ? 2029 : 2027;
        TYPE_HASH_FUNCTIONS[Type.FLOAT.code] = value -> Float.floatToIntBits((Float) value);
        TYPE_HASH_FUNCTIONS[Type.DOUBLE.code] = value -> hash(Double.doubleToLongBits((Double) value));
        TYPE_HASH_FUNCTIONS[Type.STRING.code] = NaiveHasher::hashOfByteArray;
        TYPE_HASH_FUNCTIONS[Type.UUID.code] = value -> {
            UUID uuid = (UUID) value;
            return hash(uuid.getMostSignificantBits() ^ uuid.getLeastSignificantBits());
        };
        TYPE_HASH_FUNCTIONS[Type.NULL.code] = value -> 0;
        TYPE_HASH_FUNCTIONS[Type.VECTOR.code] = NaiveHasher::hashOfVector;
    }

    static {
        Arrays.setAll(TYPE_OF_VECTOR_HASH_FUNCTIONS, idx -> value -> {
            throw new IllegalArgumentException("Unknown type with code " + idx);
        });

        TYPE_OF_VECTOR_HASH_FUNCTIONS[Type.CONTAINER.code] = value -> 0;
        TYPE_OF_VECTOR_HASH_FUNCTIONS[Type.BYTE.code] = NaiveHasher::hashOfByteArray;
        TYPE_OF_VECTOR_HASH_FUNCTIONS[Type.SHORT.code] = value -> 0;/* TODO: should be revised*/
        TYPE_OF_VECTOR_HASH_FUNCTIONS[Type.INTEGER.code] = value -> 0;/* TODO: should be revised*/
        TYPE_OF_VECTOR_HASH_FUNCTIONS[Type.LONG.code] = value -> 0;/* TODO: should be revised*/
        TYPE_OF_VECTOR_HASH_FUNCTIONS[Type.FLAG.code] = value -> 0;/* TODO: should be revised*/
        TYPE_OF_VECTOR_HASH_FUNCTIONS[Type.FLOAT.code] = value -> 0;/* TODO: should be revised*/
        TYPE_OF_VECTOR_HASH_FUNCTIONS[Type.DOUBLE.code] = value -> 0;/* TODO: should be revised*/
        TYPE_OF_VECTOR_HASH_FUNCTIONS[Type.STRING.code] = value -> 0;/* TODO: should be revised*/
        TYPE_OF_VECTOR_HASH_FUNCTIONS[Type.UUID.code] = value -> 0;/* TODO: should be revised*/
        TYPE_OF_VECTOR_HASH_FUNCTIONS[Type.NULL.code] = value -> 0;
        TYPE_OF_VECTOR_HASH_FUNCTIONS[Type.VECTOR.code] = value -> 0;
    }

    public static int hash(Type type, Object value) {
        return TYPE_HASH_FUNCTIONS[type.code].hash(value);
    }

    public static int hashOfByteArray(Object value) {
        byte[] bytes = (byte[]) value;
        int hash = 0;
        for (byte b : bytes) {
            hash = 31 * hash + b;
        }
        return hash;
    }

    public static int hashOfVector(Object value) {
        Vector vector = (Vector) value;
        return TYPE_OF_VECTOR_HASH_FUNCTIONS[vector.getType().code].hash(vector.getValue());
    }

    public static int hash(long v) {
        return (int) (v ^ (v >> 32));
    }

    @Override
    public int hash(Event event, ShardingKey shardingKey) {
        int hash = 0;
        for (HPath key : shardingKey.getKeys()) {
            Variant tagValue = key.extract(event.getPayload());
            hash = 31 * hash + ((tagValue != null) ? hash(tagValue.getType(), tagValue.getValue()) : 0);
        }
        return hash;
    }

    private interface HashFunction {
        int hash(Object value);
    }
}
