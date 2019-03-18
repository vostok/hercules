package ru.kontur.vostok.hercules.throttling;

/**
 * Weigh request to determine resources required to process it
 *
 * @author Gregory Koshelev
 */
public interface RequestWeigher<R> {
    /**
     * Weigh request where weight should be positive. If weight is undefined, than return -1
     *
     * @param request to be weighed
     * @return weight of request or -1 if it is undefined
     */
    int weigh(R request);
}
