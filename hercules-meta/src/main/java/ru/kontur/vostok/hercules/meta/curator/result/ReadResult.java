package ru.kontur.vostok.hercules.meta.curator.result;

import java.util.Optional;

/**
 * @author Gregory Koshelev
 */
public class ReadResult {
    private final Status status;
    private final byte[] data;

    private ReadResult(Status status, byte[] data) {
        this.status = status;
        this.data = data;
    }

    public boolean isSuccess() {
        return status == Status.OK || status == Status.NOT_FOUND;
    }

    public Status getStatus() {
        return status;
    }

    public Optional<byte[]> getData() {
        return Optional.ofNullable(data);
    }

    public static ReadResult notFound() {
        return new ReadResult(Status.NOT_FOUND, null);
    }

    public static ReadResult found(byte[] data) {
        return new ReadResult(Status.OK, data);
    }

    public enum Status {
        OK,
        NOT_FOUND;
    }
}