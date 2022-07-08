package ru.kontur.vostok.hercules.opentelemetry.adapter.server;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.opentelemetry.adapter.OpenTelemetryAdapterDefaults;
import ru.kontur.vostok.hercules.util.lifecycle.Lifecycle;
import ru.kontur.vostok.hercules.util.parameter.Parameter;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;
import ru.kontur.vostok.hercules.util.validation.IntegerValidators;

import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * gRPC server
 *
 * @author Innokentiy Krivonosov
 */
public class GrpcOpenTelemetryServer implements Lifecycle {
    private static final Logger LOGGER = LoggerFactory.getLogger(GrpcOpenTelemetryServer.class);

    private final int port;
    private final Server server;

    public GrpcOpenTelemetryServer(List<BindableService> services, Properties properties) {
        this.port = PropertiesUtil.get(Props.PORT, properties).get();

        ServerBuilder<?> serverBuilder = ServerBuilder.forPort(port);

        services.forEach(serverBuilder::addService);

        server = serverBuilder.build();
    }

    @Override
    public void start() {
        try {
            server.start();
            runAwaitTermination(server);
            LOGGER.info("Server started, listening on " + port);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to start due to exception", ex);
        }
    }

    private void runAwaitTermination(Server server) {
        final Thread awaitThread = new Thread(() -> {
            try {
                server.awaitTermination();
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        awaitThread.setName("grpc-server-awaiter");
        awaitThread.setDaemon(false);
        awaitThread.start();
    }

    @Override
    public boolean stop(long timeout, TimeUnit timeUnit) {
        try {
            return server.shutdown().awaitTermination(timeout, timeUnit);
        } catch (InterruptedException ex) {
            LOGGER.warn("Server stopping was interrupted", ex);
            return false;
        }
    }

    private static class Props {
        static final Parameter<Integer> PORT =
                Parameter.integerParameter("port").
                        withDefault(OpenTelemetryAdapterDefaults.DEFAULT_PORT).
                        withValidator(IntegerValidators.portValidator()).
                        build();
    }
}