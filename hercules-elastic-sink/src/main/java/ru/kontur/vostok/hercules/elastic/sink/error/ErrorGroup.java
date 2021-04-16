package ru.kontur.vostok.hercules.elastic.sink.error;

/**
 * All Elastic errors are grouped by retry ability feature
 *
 * @author Gregory Koshelev
 */
public enum ErrorGroup {
    /**
     * Error of this group is temporary.
     * <p>
     * If the document has got such an error, indexing can be safely retried for it.
     */
    RETRYABLE,
    /**
     * Error of this group cannot be fixed due to malformed document
     * or some other fundamental error in Elastic cluster.
     * <p>
     * If the document has such an error, it should be dropped.
     */
    NON_RETRYABLE,
    /**
     * Error of this group is unknown for current implementation.
     * It can be either retryable or non retryable error.
     * <p>
     * The behavior on such an error are configurable.
     * If an error of this group has been acquired, it should be classified as retryable or not.
     */
    UNKNOWN
}
