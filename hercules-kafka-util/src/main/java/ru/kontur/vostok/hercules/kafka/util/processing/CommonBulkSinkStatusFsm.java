package ru.kontur.vostok.hercules.kafka.util.processing;

import ru.kontur.vostok.hercules.util.fsm.Fsm;
import ru.kontur.vostok.hercules.util.fsm.Transition;
import ru.kontur.vostok.hercules.util.fsm.TransitionMapBuilder;

import java.util.Map;
import java.util.Objects;

/**
 * CommonBulkSinkStatusFsm
 *
 * @author Kirill Sulim
 */
public class CommonBulkSinkStatusFsm extends Fsm<CommonBulkSinkStatus> {

    private static final Map<CommonBulkSinkStatus, CommonBulkSinkStatus> INIT_COMPLETED_MAP = TransitionMapBuilder.<CommonBulkSinkStatus>start()
            .transition(Transition.of(CommonBulkSinkStatus.INIT, CommonBulkSinkStatus.RUNNING))
            .build();

    private static final Map<CommonBulkSinkStatus, CommonBulkSinkStatus> BACKEND_FAILED_MAP = TransitionMapBuilder.<CommonBulkSinkStatus>start()
            .transition(Transition.of(CommonBulkSinkStatus.RUNNING, CommonBulkSinkStatus.SUSPEND))
            .transition(Transition.of(CommonBulkSinkStatus.SUSPEND, CommonBulkSinkStatus.SUSPEND))
            .transition(Transition.of(CommonBulkSinkStatus.INIT, CommonBulkSinkStatus.INIT))
            .transition(Transition.of(CommonBulkSinkStatus.STOPPING_FROM_INIT, CommonBulkSinkStatus.STOPPING_FROM_INIT))
            .transition(Transition.of(CommonBulkSinkStatus.STOPPING_FROM_RUNNING, CommonBulkSinkStatus.STOPPING_FROM_SUSPEND))
            .transition(Transition.of(CommonBulkSinkStatus.STOPPING_FROM_SUSPEND, CommonBulkSinkStatus.STOPPING_FROM_SUSPEND))
            .build();

    private static final Map<CommonBulkSinkStatus, CommonBulkSinkStatus> BACKEND_BACK_MAP = TransitionMapBuilder.<CommonBulkSinkStatus>start()
            .transition(Transition.of(CommonBulkSinkStatus.SUSPEND, CommonBulkSinkStatus.RUNNING))
            .transition(Transition.of(CommonBulkSinkStatus.RUNNING, CommonBulkSinkStatus.RUNNING))
            .transition(Transition.of(CommonBulkSinkStatus.INIT, CommonBulkSinkStatus.INIT))
            .transition(Transition.of(CommonBulkSinkStatus.STOPPING_FROM_INIT, CommonBulkSinkStatus.STOPPING_FROM_INIT))
            .transition(Transition.of(CommonBulkSinkStatus.STOPPING_FROM_RUNNING, CommonBulkSinkStatus.STOPPING_FROM_RUNNING))
            .transition(Transition.of(CommonBulkSinkStatus.STOPPING_FROM_SUSPEND, CommonBulkSinkStatus.STOPPING_FROM_SUSPEND))
            .build();

    private static final Map<CommonBulkSinkStatus, CommonBulkSinkStatus> STOP_MAP = TransitionMapBuilder.<CommonBulkSinkStatus>start()
            .transition(Transition.of(CommonBulkSinkStatus.INIT, CommonBulkSinkStatus.STOPPING_FROM_INIT))
            .transition(Transition.of(CommonBulkSinkStatus.RUNNING, CommonBulkSinkStatus.STOPPING_FROM_RUNNING))
            .transition(Transition.of(CommonBulkSinkStatus.SUSPEND, CommonBulkSinkStatus.STOPPING_FROM_SUSPEND))
            .build();

    public CommonBulkSinkStatusFsm() {
        super(CommonBulkSinkStatus.INIT);
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
        CommonBulkSinkStatus state = getState();
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
