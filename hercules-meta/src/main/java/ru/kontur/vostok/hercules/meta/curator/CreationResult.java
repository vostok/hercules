package ru.kontur.vostok.hercules.meta.curator;

/**
 * @author Gregory Koshelev
 */
public class CreationResult {
    private final Status status;

    private CreationResult(Status status) {
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
        ALREADY_EXIST,
        UNKNOWN;
    }

    public static CreationResult ok() {
        return OK;
    }

    public static CreationResult alreadyExist() {
        return ALREADY_EXIST;
    }

    public static CreationResult unknown() {
        return UNKNOWN;
    }

    private static final CreationResult OK = new CreationResult(Status.OK);
    private static final CreationResult ALREADY_EXIST = new CreationResult(Status.ALREADY_EXIST);
    private static final CreationResult UNKNOWN = new CreationResult(Status.UNKNOWN);
}
