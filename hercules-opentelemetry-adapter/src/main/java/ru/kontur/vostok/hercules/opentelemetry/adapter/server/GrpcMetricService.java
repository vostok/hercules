package ru.kontur.vostok.hercules.opentelemetry.adapter.server;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import io.opentelemetry.proto.collector.metrics.v1.ExportMetricsServiceRequest;
import io.opentelemetry.proto.collector.metrics.v1.ExportMetricsServiceResponse;
import io.opentelemetry.proto.collector.metrics.v1.MetricsServiceGrpc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.gate.client.GateSender;
import ru.kontur.vostok.hercules.gate.client.GateStatus;
import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.opentelemetry.adapter.converters.MetricConverter;
import ru.kontur.vostok.hercules.opentelemetry.adapter.metrics.GrpcServiceMetrics;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.util.parameter.Parameter;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;
import ru.kontur.vostok.hercules.util.time.TimeSource;

import java.util.List;
import java.util.Properties;

/**
 * Metric service for receiving metrics from Applications instrumented with OpenTelemetry.
 * Extend generated gRPC service stub.
 *
 * @author Innokentiy Krivonosov
 * @see <a href="https://github.com/open-telemetry/opentelemetry-proto/blob/main/opentelemetry/proto/collector/metrics/v1/metrics_service.proto">
 * OpenTelemetry metrics_service.proto</a>
 */
public class GrpcMetricService extends MetricsServiceGrpc.MetricsServiceImplBase {
    private static final Logger LOGGER = LoggerFactory.getLogger(GrpcMetricService.class);

    private final GateSender gateSender;
    private final String stream;
    private final GrpcServiceMetrics metrics;

    public GrpcMetricService(GateSender gateSender, Properties properties, MetricsCollector metricsCollector) {
        this.gateSender = gateSender;
        this.metrics = new GrpcServiceMetrics(getClass().getSimpleName(), TimeSource.SYSTEM, metricsCollector);
        this.stream = PropertiesUtil.get(Props.STREAM, properties).get();
    }

    /**
     * The service method for a unary RPC call
     *
     * @param request          metric request
     * @param responseObserver response observer with empty response
     */
    @Override
    public void export(
            ExportMetricsServiceRequest request,
            StreamObserver<ExportMetricsServiceResponse> responseObserver
    ) {
        long convertingEventsStartedAtMs = metrics.startMilliseconds();
        List<Event> events = MetricConverter.convert(request.getResourceMetricsList());

        long sendingEventsStartedAtMs = metrics.convertingEnded(convertingEventsStartedAtMs);
        GateStatus status = gateSender.send(events, false, stream);

        if (status == GateStatus.OK) {
            responseObserver.onNext(ExportMetricsServiceResponse.getDefaultInstance());
            responseObserver.onCompleted();
            metrics.markDelivered(events, sendingEventsStartedAtMs);
        } else {
            responseObserver.onError(getErrorStatus(status).asException());
            LOGGER.error("Got " + status + " error from Gate while sending " + events.size() + " events to the stream " + stream);
            metrics.markFailed(events, sendingEventsStartedAtMs);
        }
    }

    private Status getErrorStatus(GateStatus status) {
        switch (status) {
            case BAD_REQUEST:
                return Status.INTERNAL;
            case GATE_UNAVAILABLE:
                return Status.UNAVAILABLE;
            default:
                return Status.UNKNOWN;
        }
    }

    private static class Props {
        static final Parameter<String> STREAM =
                Parameter.stringParameter("stream")
                        .required()
                        .build();
    }
}
