package ru.kontur.vostok.hercules.kafka.util.processing;

import ru.kontur.vostok.hercules.util.fsm.Fsm;
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
            .transition(CommonBulkSinkStatus.INIT, CommonBulkSinkStatus.RUNNING)
            .build();

    private static final Map<CommonBulkSinkStatus, CommonBulkSinkStatus> BACKEND_FAILED_MAP = TransitionMapBuilder.<CommonBulkSinkStatus>start()
            .transition(CommonBulkSinkStatus.RUNNING, CommonBulkSinkStatus.SUSPEND)
            .transition(CommonBulkSinkStatus.SUSPEND, CommonBulkSinkStatus.SUSPEND)
            .transition(CommonBulkSinkStatus.INIT, CommonBulkSinkStatus.INIT)
            .transition(CommonBulkSinkStatus.STOPPING_FROM_INIT, CommonBulkSinkStatus.STOPPING_FROM_INIT)
            .transition(CommonBulkSinkStatus.STOPPING_FROM_RUNNING, CommonBulkSinkStatus.STOPPING_FROM_SUSPEND)
            .transition(CommonBulkSinkStatus.STOPPING_FROM_SUSPEND, CommonBulkSinkStatus.STOPPING_FROM_SUSPEND)
            .build();

    private static final Map<CommonBulkSinkStatus, CommonBulkSinkStatus> BACKEND_BACK_MAP = TransitionMapBuilder.<CommonBulkSinkStatus>start()
            .transition(CommonBulkSinkStatus.SUSPEND, CommonBulkSinkStatus.RUNNING)
            .transition(CommonBulkSinkStatus.RUNNING, CommonBulkSinkStatus.RUNNING)
            .transition(CommonBulkSinkStatus.INIT, CommonBulkSinkStatus.INIT)
            .transition(CommonBulkSinkStatus.STOPPING_FROM_INIT, CommonBulkSinkStatus.STOPPING_FROM_INIT)
            .transition(CommonBulkSinkStatus.STOPPING_FROM_RUNNING, CommonBulkSinkStatus.STOPPING_FROM_RUNNING)
            .transition(CommonBulkSinkStatus.STOPPING_FROM_SUSPEND, CommonBulkSinkStatus.STOPPING_FROM_SUSPEND)
            .build();

    private static final Map<CommonBulkSinkStatus, CommonBulkSinkStatus> STOP_MAP = TransitionMapBuilder.<CommonBulkSinkStatus>start()
            .transition(CommonBulkSinkStatus.INIT, CommonBulkSinkStatus.STOPPING_FROM_INIT)
            .transition(CommonBulkSinkStatus.RUNNING, CommonBulkSinkStatus.STOPPING_FROM_RUNNING)
            .transition(CommonBulkSinkStatus.SUSPEND, CommonBulkSinkStatus.STOPPING_FROM_SUSPEND)
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
