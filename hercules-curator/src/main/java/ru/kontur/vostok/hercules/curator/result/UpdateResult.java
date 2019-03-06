package ru.kontur.vostok.hercules.curator.result;

/**
 * @author Gregory Koshelev
 */
public class UpdateResult {
    private final Status status;

    private UpdateResult(Status status) {
        this.status = status;
    }

    public boolean isSuccess() {
        return status == Status.OK;
    }

    public Status status() {
        return status;
    }

    public enum Status {
        OK,
        NOT_EXIST;
    }

    public static UpdateResult ok() {
        return OK;
    }

    public static UpdateResult notExist() {
        return NOT_EXIST;
    }

    private static final UpdateResult OK = new UpdateResult(Status.OK);
    private static final UpdateResult NOT_EXIST = new UpdateResult(Status.NOT_EXIST);
}
