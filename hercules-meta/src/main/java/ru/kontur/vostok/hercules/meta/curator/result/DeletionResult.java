package ru.kontur.vostok.hercules.meta.curator.result;

/**
 * @author Gregory Koshelev
 */
public class DeletionResult {
    private final Status status;

    private DeletionResult(Status status) {
        this.status = status;
    }

    public boolean isSuccess() {
        return status == Status.OK;
    }

    public Status getStatus() {
        return status;
    }

    public enum Status {
        OK,
        NOT_EXIST;
    }

    public static DeletionResult ok() {
        return OK;
    }

    public static DeletionResult notExist() {
        return NOT_EXIST;
    }

    private static final DeletionResult OK = new DeletionResult(Status.OK);
    private static final DeletionResult NOT_EXIST = new DeletionResult(Status.NOT_EXIST);
}
