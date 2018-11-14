package ru.kontur.vostok.hercules.kafka.util.processing;

import ru.kontur.vostok.hercules.util.fsm.Fsm;
import ru.kontur.vostok.hercules.util.fsm.Transition;
import ru.kontur.vostok.hercules.util.fsm.TransitionMapBuilder;

import java.util.Map;
import java.util.Objects;

/**
 * SinkStatusFsm - FSM for {@link SinkStatus}
 *
 * @author Kirill Sulim
 */
public class SinkStatusFsm extends Fsm<SinkStatus> {

    private static final Map<SinkStatus, SinkStatus> INIT_COMPLETED_MAP = TransitionMapBuilder.<SinkStatus>start()
            .transition(Transition.of(SinkStatus.INIT, SinkStatus.RUNNING))
            .build();

    private static final Map<SinkStatus, SinkStatus> BACKEND_FAILED_MAP = TransitionMapBuilder.<SinkStatus>start()
            .transition(Transition.of(SinkStatus.RUNNING, SinkStatus.SUSPEND))
            .transition(Transition.of(SinkStatus.SUSPEND, SinkStatus.SUSPEND))
            .transition(Transition.of(SinkStatus.INIT, SinkStatus.INIT))
            .transition(Transition.of(SinkStatus.STOPPING_FROM_INIT, SinkStatus.STOPPING_FROM_INIT))
            .transition(Transition.of(SinkStatus.STOPPING_FROM_RUNNING, SinkStatus.STOPPING_FROM_SUSPEND))
            .transition(Transition.of(SinkStatus.STOPPING_FROM_SUSPEND, SinkStatus.STOPPING_FROM_SUSPEND))
            .build();

    private static final Map<SinkStatus, SinkStatus> BACKEND_BACK_MAP = TransitionMapBuilder.<SinkStatus>start()
            .transition(Transition.of(SinkStatus.SUSPEND, SinkStatus.RUNNING))
            .transition(Transition.of(SinkStatus.RUNNING, SinkStatus.RUNNING))
            .transition(Transition.of(SinkStatus.INIT, SinkStatus.INIT))
            .transition(Transition.of(SinkStatus.STOPPING_FROM_INIT, SinkStatus.STOPPING_FROM_INIT))
            .transition(Transition.of(SinkStatus.STOPPING_FROM_RUNNING, SinkStatus.STOPPING_FROM_RUNNING))
            .transition(Transition.of(SinkStatus.STOPPING_FROM_SUSPEND, SinkStatus.STOPPING_FROM_SUSPEND))
            .build();

    private static final Map<SinkStatus, SinkStatus> STOP_MAP = TransitionMapBuilder.<SinkStatus>start()
            .transition(Transition.of(SinkStatus.INIT, SinkStatus.STOPPING_FROM_INIT))
            .transition(Transition.of(SinkStatus.RUNNING, SinkStatus.STOPPING_FROM_RUNNING))
            .transition(Transition.of(SinkStatus.SUSPEND, SinkStatus.STOPPING_FROM_SUSPEND))
            .build();

    public SinkStatusFsm() {
        super(SinkStatus.INIT);
    }

    public void markInitCompleted() {
        if (Objects.isNull(makeTransition(INIT_COMPLETED_MAP))) {
            throw new IllegalStateException(String.format("Cannot change status %s", getState()));
        }
    }

    public void markBackendFailed() {
        if (Objects.isNull(makeTransition(BACKEND_FAILED_MAP))) {
            throw new IllegalStateException(String.format("Cannot change status %s", getState()));
        }
    }

    public void markBackendAlive() {
        if (Objects.isNull(makeTransition(BACKEND_BACK_MAP))) {
            throw new IllegalStateException(String.format("Cannot change status %s", getState()));
        }
    }

    public void stop() {
        if (Objects.isNull(makeTransition(STOP_MAP))) {
            throw new IllegalStateException(String.format("Cannot change status %s", getState()));
        }
    }

    public boolean isRunning() {
        SinkStatus state = getState();
        switch (state) {
            case INIT:
            case RUNNING:
            case SUSPEND:
                return true;
            case STOPPING_FROM_INIT:
            case STOPPING_FROM_RUNNING:
            case STOPPING_FROM_SUSPEND:
            case STOPPED:
                return false;
            default:
                throw new IllegalStateException(String.format("Unknown status %s", state));
        }
    }
}
