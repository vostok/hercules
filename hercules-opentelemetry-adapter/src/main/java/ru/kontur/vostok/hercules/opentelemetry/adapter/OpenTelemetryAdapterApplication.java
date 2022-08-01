package ru.kontur.vostok.hercules.opentelemetry.adapter;

import ru.kontur.vostok.hercules.application.Application;
import ru.kontur.vostok.hercules.configuration.Scopes;
import ru.kontur.vostok.hercules.gate.client.GateSender;
import ru.kontur.vostok.hercules.health.CommonMetrics;
import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.opentelemetry.adapter.server.GrpcOpenTelemetryServer;
import ru.kontur.vostok.hercules.opentelemetry.adapter.server.GrpcTraceService;
import ru.kontur.vostok.hercules.undertow.util.servers.DaemonHttpServer;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;

import java.util.List;
import java.util.Properties;

/**
 * @author Innokentiy Krivonosov
 */
public class OpenTelemetryAdapterApplication {
    public static void main(String[] args) {
        Application.run("Hercules OpenTelemetry Adapter", "opentelemetry-adapter", args, (properties, container) -> {
            Properties metricsProperties = PropertiesUtil.ofScope(properties, Scopes.METRICS);
            Properties gateClientProperties = PropertiesUtil.ofScope(properties, Scopes.GATE_CLIENT);
            Properties grpcServerProperties = PropertiesUtil.ofScope(properties, "grpc.server");
            Properties grpcTraceServiceProperties = PropertiesUtil.ofScope(properties, "trace.service");
            Properties httpServerProperties = PropertiesUtil.ofScope(properties, Scopes.HTTP_SERVER);

            MetricsCollector metricsCollector = container.register(new MetricsCollector(metricsProperties));
            CommonMetrics.registerCommonMetrics(metricsCollector);

            GateSender gateSender = container.register(new GateSender(gateClientProperties));

            GrpcTraceService grpcTraceService = new GrpcTraceService(gateSender, grpcTraceServiceProperties, metricsCollector);

            container.register(new GrpcOpenTelemetryServer(List.of(grpcTraceService), grpcServerProperties, metricsCollector));
            container.register(new DaemonHttpServer(httpServerProperties, metricsCollector));
        });
    }
}
