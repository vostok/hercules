package ru.kontur.vostok.hercules.opentelemetry.adapter.metrics;

import io.grpc.ForwardingServerCall;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.Status;

/**
 * Encapsulates a single call to receive a response code
 *
 * @author Innokentiy Krivonosov
 */
public class MonitoringServerCall<R, S> extends ForwardingServerCall.SimpleForwardingServerCall<R, S> {
    private Status.Code responseCode = Status.Code.UNKNOWN;

    protected MonitoringServerCall(ServerCall<R, S> delegate) {
        super(delegate);
    }

    public Status.Code getResponseCode() {
        return this.responseCode;
    }

    @Override
    public void close(Status status, Metadata metadata) {
        this.responseCode = status.getCode();
        super.close(status, metadata);
    }
}
