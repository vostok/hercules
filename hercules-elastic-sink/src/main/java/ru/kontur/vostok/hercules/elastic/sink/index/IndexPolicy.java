package ru.kontur.vostok.hercules.elastic.sink.index;

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
    ILM,
    /**
     * Static index name
     */
    STATIC;
}
