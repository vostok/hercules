package ru.kontur.vostok.hercules.kafka.util.processing;

import ru.kontur.vostok.hercules.health.IHaveStatusCode;
import ru.kontur.vostok.hercules.util.fsm.State;

/**
 * SinkStatus - finite state machine for common sink
 *
 * @author Kirill Sulim
 */
public enum SinkStatus implements IHaveStatusCode, State {
    /**
     * Sink in initialization execute
     */
    INIT(null),

    /**
     * Sink running normally
     */
    RUNNING(0),

    /**
     * Sink is suspended due backend services fail
     */
    SUSPEND(1),

    /**
     * Sink is stopping after unsuccessful initialization
     */
    STOPPING_FROM_INIT(null),

    /**
     * Sink is stopping after running normally
     */
    STOPPING_FROM_RUNNING(0),

    /**
     * Sink is stopping after being suspended
     */
    STOPPING_FROM_SUSPEND(1),

    /**
     * Sink is stopped
     */
    STOPPED(null),
    ;

    private final Integer statusCode;

    SinkStatus(Integer statusCode) {
        this.statusCode = statusCode;
    }

    @Override
    public Integer getStatusCode() {
        return statusCode;
    }
}
