package ru.kontur.vostok.hercules.tracing.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.application.Application;
import ru.kontur.vostok.hercules.configuration.PropertiesLoader;
import ru.kontur.vostok.hercules.configuration.Scopes;
import ru.kontur.vostok.hercules.configuration.util.ArgsParser;
import ru.kontur.vostok.hercules.health.CommonMetrics;
import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.http.HttpServer;
import ru.kontur.vostok.hercules.http.handler.RouteHandler;
import ru.kontur.vostok.hercules.json.format.EventToJsonFormatter;
import ru.kontur.vostok.hercules.undertow.util.UndertowHttpServer;
import ru.kontur.vostok.hercules.undertow.util.handlers.InstrumentedRouteHandlerBuilder;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * Tracing API Application
 *
 * @author Gregory Koshelev
 */
public class TracingApiApplication {
    private static final Logger LOGGER = LoggerFactory.getLogger(TracingApiApplication.class);

    private static MetricsCollector metricsCollector;
    private static TracingReader tracingReader;
    private static EventToJsonFormatter eventFormatter;
    private static HttpServer server;

    public static void main(String[] args) {
        long start = System.currentTimeMillis();

        try {
            Application.run("Hercules Tracing API", "tracing-api", args);

            Map<String, String> parameters = ArgsParser.parse(args);

            Properties properties = PropertiesLoader.load(parameters.getOrDefault("application.properties", "file://application.properties"));

            Properties metricsProperties = PropertiesUtil.ofScope(properties, Scopes.METRICS);
            Properties httpServerProperties = PropertiesUtil.ofScope(properties, Scopes.HTTP_SERVER);
            Properties readerProperties = PropertiesUtil.ofScope(properties, "reader");

            metricsCollector = new MetricsCollector(metricsProperties);
            metricsCollector.start();
            CommonMetrics.registerCommonMetrics(metricsCollector);

            tracingReader = TracingReader.createTracingReader(readerProperties);

            eventFormatter = new EventToJsonFormatter(PropertiesUtil.ofScope(properties, "tracing.format"));

            server = createHttpServer(httpServerProperties);
            server.start();
        } catch (Throwable t) {
            LOGGER.error("Error on starting Tracing API", t);
            shutdown();
            return;
        }

        Runtime.getRuntime().addShutdownHook(new Thread(TracingApiApplication::shutdown));

        LOGGER.info("Tracing API started for {} millis", System.currentTimeMillis() - start);
    }

    private static void shutdown() {
        long start = System.currentTimeMillis();
        LOGGER.info("Started Tracing API shutdown");
        try {
            if (server != null) {
                server.stop(5_000, TimeUnit.MILLISECONDS);
            }
        } catch (Throwable t) {
            LOGGER.error("Error on stopping server", t);
            //TODO: Process error
        }

        try {
            if (tracingReader != null) {
                tracingReader.close();
            }
        } catch (Throwable t) {
            LOGGER.error("Error on stopping tracing reader", t);
            //TODO: Process error
        }

        try {
            if (metricsCollector != null) {
                metricsCollector.stop();
            }
        } catch (Throwable t) {
            LOGGER.error("Error on metrics collector shutdown", t);
            //TODO: Process error
        }

        LOGGER.info("Finished Tracing API shutdown for {} millis", System.currentTimeMillis() - start);
    }

    private static HttpServer createHttpServer(Properties httpServerProperties) {
        RouteHandler handler = new InstrumentedRouteHandlerBuilder(httpServerProperties, metricsCollector).
                get("/trace", new GetTraceHandler(tracingReader, eventFormatter)).
                build();

        return new UndertowHttpServer(
                Application.application().getConfig().getHost(),
                Application.application().getConfig().getPort(),
                httpServerProperties,
                handler);
    }
}
