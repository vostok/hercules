package ru.kontur.vostok.hercules.curator.result;

/**
 * @author Gregory Koshelev
 */
public class CreationResult {
    private final Status status;
    private final String path;

    private CreationResult(Status status, String path) {
        this.status = status;
        this.path = path;
    }

    public boolean isSuccess() {
        return status == Status.OK;
    }

    public Status getStatus() {
        return status;
    }

    public enum Status {
        OK,
        ALREADY_EXIST;
    }

    public static CreationResult ok(String path) {
        return new CreationResult(Status.OK, path);
    }

    public static CreationResult alreadyExist(String path) {
        return new CreationResult(Status.ALREADY_EXIST, path);
    }
}
