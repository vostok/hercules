package ru.kontur.vostok.hercules.elastic.sink;

/**
 * @author Gregory Koshelev
 */
public enum IndexPolicy {
    /**
     * Per day indices
     */
    DAILY,
    /**
     * Index Lifecycle management is used
     */
    ILM;
}
