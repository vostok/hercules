package ru.kontur.vostok.hercules.gate;

/**
 * @author Gregory Koshelev
 */
public enum PartitionerType {
    KAFKA_DEFAULT,
    BATCHED,
    RANDOM,
    ROUND_ROBIN;
}
