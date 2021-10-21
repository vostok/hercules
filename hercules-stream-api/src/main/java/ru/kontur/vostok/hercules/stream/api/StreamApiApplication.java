package ru.kontur.vostok.hercules.stream.api;

import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import ru.kontur.vostok.hercules.application.Application;
import ru.kontur.vostok.hercules.auth.AdminAuthManager;
import ru.kontur.vostok.hercules.auth.AuthManager;
import ru.kontur.vostok.hercules.auth.AuthProvider;
import ru.kontur.vostok.hercules.auth.wrapper.OrdinaryAuthHandlerWrapper;
import ru.kontur.vostok.hercules.configuration.Scopes;
import ru.kontur.vostok.hercules.curator.CuratorClient;
import ru.kontur.vostok.hercules.health.CommonMetrics;
import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.http.HttpServer;
import ru.kontur.vostok.hercules.http.handler.HandlerWrapper;
import ru.kontur.vostok.hercules.http.handler.HttpHandler;
import ru.kontur.vostok.hercules.http.handler.RouteHandler;
import ru.kontur.vostok.hercules.kafka.util.serialization.VoidDeserializer;
import ru.kontur.vostok.hercules.meta.stream.StreamRepository;
import ru.kontur.vostok.hercules.undertow.util.UndertowHttpServer;
import ru.kontur.vostok.hercules.http.handler.InstrumentedRouteHandlerBuilder;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;

import java.util.Collections;
import java.util.Properties;

public class StreamApiApplication {
    private static CuratorClient curatorClient;
    private static MetricsCollector metricsCollector;
    private static AuthManager authManager;
    private static ConsumerPool<Void, byte[]> consumerPool;
    private static StreamReadRequestProcessor streamReadRequestProcessor;

    public static void main(String[] args) {
        Application.run("Hercules Stream API", "stream-api", args, (properties, container) -> {
            Properties curatorProperties = PropertiesUtil.ofScope(properties, Scopes.CURATOR);
            Properties metricsProperties = PropertiesUtil.ofScope(properties, Scopes.METRICS);
            Properties httpServerProperties = PropertiesUtil.ofScope(properties, Scopes.HTTP_SERVER);
            Properties consumerPoolProperties = PropertiesUtil.ofScope(properties, "stream.api.pool");
            Properties streamReaderProperties = PropertiesUtil.ofScope(properties, "stream.api.reader");
            Properties requestProcessorProperties = PropertiesUtil.ofScope(properties, "stream.api.stream.read.request.processor");

            curatorClient = container.register(new CuratorClient(curatorProperties));

            metricsCollector = container.register(new MetricsCollector(metricsProperties));
            CommonMetrics.registerCommonMetrics(metricsCollector);

            authManager = container.register(new AuthManager(curatorClient));

            consumerPool = container.register(new ConsumerPool<>(
                    consumerPoolProperties,
                    new VoidDeserializer(),
                    new ByteArrayDeserializer(),
                    metricsCollector));

            StreamReader streamReader = new StreamReader(streamReaderProperties, consumerPool, metricsCollector);
            streamReadRequestProcessor = new StreamReadRequestProcessor(requestProcessorProperties, streamReader, metricsCollector);

            container.register(createHttpServer(httpServerProperties));
        });
    }

    private static HttpServer createHttpServer(Properties httpServerProperties) {
        StreamRepository repository = new StreamRepository(curatorClient);

        AuthProvider authProvider = new AuthProvider(new AdminAuthManager(Collections.emptySet()), authManager, metricsCollector);
        HandlerWrapper authHandlerWrapper = new OrdinaryAuthHandlerWrapper(authProvider);

        HttpHandler readStreamHandler = authHandlerWrapper.wrap(
                new StreamReadHandler(authProvider, repository, streamReadRequestProcessor));
        HttpHandler seekToEndHandler = authHandlerWrapper.wrap(
                new SeekToEndHandler(authProvider, repository, consumerPool));

        RouteHandler handler = new InstrumentedRouteHandlerBuilder(httpServerProperties, metricsCollector).
                post("/stream/read", readStreamHandler).
                get("/stream/seekToEnd", seekToEndHandler).
                build();

        return new UndertowHttpServer(
                Application.application().getConfig().getHost(),
                Application.application().getConfig().getPort(),
                httpServerProperties,
                handler);
    }
}
