package ru.kontur.vostok.hercules.meta.curator;

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

    public Status getStatus() {
        return status;
    }

    public enum Status {
        OK,
        NOT_EXIST,
        UNKNOWN;
    }

    public static UpdateResult ok() {
        return OK;
    }

    public static UpdateResult notExist() {
        return NOT_EXIST;
    }

    public static UpdateResult unknown() {
        return UNKNOWN;
    }

    private static final UpdateResult OK = new UpdateResult(Status.OK);
    private static final UpdateResult NOT_EXIST = new UpdateResult(Status.NOT_EXIST);
    private static final UpdateResult UNKNOWN = new UpdateResult(Status.UNKNOWN);
}
