package ru.kontur.vostok.hercules.opentelemetry.adapter.metrics;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.util.time.TimeSource;

/**
 * A gRPC server interceptor that will collect metrics.
 *
 * @author Innokentiy Krivonosov
 */
public class MonitoringServerInterceptor implements ServerInterceptor {
    private final ServerCallMetrics metrics;

    public MonitoringServerInterceptor(String serviceName, TimeSource time, MetricsCollector metricsCollector) {
        this.metrics = new ServerCallMetrics(serviceName, time, metricsCollector);
    }

    @Override
    public <R, S> ServerCall.Listener<R> interceptCall(
            ServerCall<R, S> call, Metadata metadata, ServerCallHandler<R, S> next
    ) {
        long serverCallStartedAtMs = metrics.startMilliseconds();

        MonitoringServerCall<R, S> monitoringCall = new MonitoringServerCall<>(call);

        return new MonitoringServerCallListener<>(
                next.startCall(monitoringCall, metadata), metrics, serverCallStartedAtMs, monitoringCall::getResponseCode
        );
    }
}
