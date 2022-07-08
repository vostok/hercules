package ru.kontur.vostok.hercules.opentelemetry.adapter.server;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceResponse;
import io.opentelemetry.proto.collector.trace.v1.TraceServiceGrpc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.gate.client.GateSender;
import ru.kontur.vostok.hercules.gate.client.GateStatus;
import ru.kontur.vostok.hercules.opentelemetry.adapter.converters.TraceConverter;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.util.parameter.Parameter;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;

import java.util.List;
import java.util.Properties;

/**
 * Trace service for receiving spans from Applications instrumented with OpenTelemetry.
 * Extend generated gRPC service stub.
 *
 * @author Innokentiy Krivonosov
 * @see <a href="https://github.com/open-telemetry/opentelemetry-proto/blob/main/opentelemetry/proto/collector/trace/v1/trace_service.proto">
 * OpenTelemetry trace_service.proto</a>
 */
public class GrpcTraceService extends TraceServiceGrpc.TraceServiceImplBase {
    private static final Logger LOGGER = LoggerFactory.getLogger(GrpcTraceService.class);

    private final GateSender gateSender;
    private final String stream;

    public GrpcTraceService(GateSender gateSender, Properties properties) {
        this.gateSender = gateSender;
        this.stream = PropertiesUtil.get(Props.STREAM, properties).get();
    }

    /**
     * The service method for a unary RPC call
     *
     * @param request          trace request
     * @param responseObserver response observer with empty response
     */
    @Override
    public void export(
            ExportTraceServiceRequest request,
            StreamObserver<ExportTraceServiceResponse> responseObserver
    ) {
        List<Event> events = TraceConverter.convert(request.getResourceSpansList());

        GateStatus status = gateSender.send(events, false, stream);

        switch (status) {
            case OK:
                responseObserver.onNext(ExportTraceServiceResponse.getDefaultInstance());
                responseObserver.onCompleted();
                return;
            case BAD_REQUEST:
                responseObserver.onError(Status.INTERNAL.asException());
                LOGGER.error("Got bad request from Gate while sending " + events.size() + " events to the stream " + stream);
                return;
            case GATE_UNAVAILABLE:
                responseObserver.onError(Status.UNAVAILABLE.asException());
                LOGGER.warn("Gate is unavailable: didn't send " + events.size() + " events to the stream " + stream);
                return;
            default:
                responseObserver.onError(Status.UNKNOWN.asException());
                LOGGER.error("Gate unknown status " + status + ", didn't send " + events.size() + " events to the stream " + stream);
        }
    }

    private static class Props {
        static final Parameter<String> STREAM =
                Parameter.stringParameter("stream").
                        required().
                        build();
    }
}
