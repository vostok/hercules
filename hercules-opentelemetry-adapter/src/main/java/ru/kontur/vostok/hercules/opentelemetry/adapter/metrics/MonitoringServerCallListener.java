package ru.kontur.vostok.hercules.opentelemetry.adapter.metrics;

import io.grpc.ForwardingServerCallListener;
import io.grpc.ServerCall;
import io.grpc.Status;

import java.util.function.Supplier;

/**
 * A simple forwarding server call listener that collects metrics.
 *
 * @author Innokentiy Krivonosov
 */
class MonitoringServerCallListener<R> extends ForwardingServerCallListener.SimpleForwardingServerCallListener<R> {
    private final ServerCallMetrics metrics;
    private final long serverCallStartedAtMs;
    private final Supplier<Status.Code> responseCodeSupplier;

    MonitoringServerCallListener(
            ServerCall.Listener<R> delegate,
            ServerCallMetrics metrics,
            long serverCallStartedAtMs,
            Supplier<Status.Code> responseCodeSupplier
    ) {
        super(delegate);
        this.metrics = metrics;
        this.serverCallStartedAtMs = serverCallStartedAtMs;
        this.responseCodeSupplier = responseCodeSupplier;
    }

    @Override
    public void onHalfClose() {
        metrics.receivingEnded(serverCallStartedAtMs);
        super.onHalfClose();
    }

    @Override
    public void onComplete() {
        metrics.serverCallEnded(this.responseCodeSupplier.get(), serverCallStartedAtMs);
        super.onComplete();
    }

    @Override
    public void onCancel() {
        metrics.serverCallEnded(Status.Code.CANCELLED, serverCallStartedAtMs);
        super.onCancel();
    }
}
