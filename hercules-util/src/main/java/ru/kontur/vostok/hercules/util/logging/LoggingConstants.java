package ru.kontur.vostok.hercules.util.logging;

/**
 * LoggingConstants
 *
 * @author Kirill Sulim
 */
public final class LoggingConstants {

    public static final String RECEIVED_EVENT_LOGGER_NAME = "RECEIVED_EVENTS";
    public static final String PROCESSED_EVENT_LOGGER_NAME = "PROCESSED_EVENTS";
    public static final String DROPPED_EVENT_LOGGER_NAME = "DROPPED_EVENTS";

    public static final String RECEIVED_EVENT_TRACE_TEMPLATE = "[received-event], {}";
    public static final String PROCESSED_EVENT_TRACE_TEMPLATE = "[processed-event], {}";
    public static final String DROPPED_EVENT_TRACE_TEMPLATE = "[dropped-event], {}";


    private LoggingConstants() {
    }
}
