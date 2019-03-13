package ru.kontur.vostok.hercules.curator.result;

import ru.kontur.vostok.hercules.curator.ZkUtil;

/**
 * @author Gregory Koshelev
 */
public class CreationResult {
    private final Status status;
    private final String path;
    private final String node;

    private CreationResult(Status status, String path) {
        this.status = status;
        this.path = path;
        this.node = ZkUtil.getLeafNodeFromPath(path);
    }

    public boolean isSuccess() {
        return status == Status.OK;
    }

    public Status status() {
        return status;
    }

    public String path() {
        return path;
    }

    public String node() {
        return node;
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
