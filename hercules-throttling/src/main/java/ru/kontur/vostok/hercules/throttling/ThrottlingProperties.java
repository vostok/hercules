package ru.kontur.vostok.hercules.throttling;

/**
 * @author Gregory Koshelev
 */
public final class ThrottlingProperties {
    /**
     * total amount of throttled resources
     */
    public static final String CAPACITY = "capacity";
    /**
     * request's timeout. Timeout is measured in milliseconds
     */
    public static final String REQUEST_TIMEOUT = "requestTimeout";
}
