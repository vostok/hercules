package ru.kontur.vostok.hercules.elastic.adapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.application.Application;
import ru.kontur.vostok.hercules.configuration.PropertiesLoader;
import ru.kontur.vostok.hercules.configuration.Scopes;
import ru.kontur.vostok.hercules.configuration.util.ArgsParser;
import ru.kontur.vostok.hercules.elastic.adapter.gate.GateSender;
import ru.kontur.vostok.hercules.elastic.adapter.handler.BulkHandler;
import ru.kontur.vostok.hercules.elastic.adapter.handler.IndexHandler;
import ru.kontur.vostok.hercules.elastic.adapter.index.IndexManager;
import ru.kontur.vostok.hercules.health.CommonMetrics;
import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.http.HttpServer;
import ru.kontur.vostok.hercules.http.handler.RouteHandler;
import ru.kontur.vostok.hercules.undertow.util.UndertowHttpServer;
import ru.kontur.vostok.hercules.undertow.util.handlers.InstrumentedRouteHandlerBuilder;
import ru.kontur.vostok.hercules.util.parameter.Parameter;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * @author Gregory Koshelev
 */
public class ElasticAdapterApplication {
    private static final Logger LOGGER = LoggerFactory.getLogger(ElasticAdapterApplication.class);

    private static MetricsCollector metricsCollector;
    private static GateSender gateSender;
    private static IndexManager indexManager;
    private static HttpServer httpServer;

    public static void main(String[] args) {
        long start = System.currentTimeMillis();

        try {
            Application.run("Hercules Elastic Adapter", "elastic-adapter", args);

            Map<String, String> parameters = ArgsParser.parse(args);

            Properties properties = PropertiesLoader.load(parameters.getOrDefault("application.properties", "file://application.properties"));

            Properties metricsProperties = PropertiesUtil.ofScope(properties, Scopes.METRICS);
            Properties gateClientProperties = PropertiesUtil.ofScope(properties, Scopes.GATE_CLIENT);
            Properties indexManagerProperties = PropertiesUtil.ofScope(properties, "index.manager");
            Properties httpServerProperties = PropertiesUtil.ofScope(properties, Scopes.HTTP_SERVER);

            metricsCollector = new MetricsCollector(metricsProperties);
            metricsCollector.start();
            CommonMetrics.registerCommonMetrics(metricsCollector);

            gateSender = new GateSender(gateClientProperties);

            indexManager = new IndexManager(indexManagerProperties);

            httpServer = createHttpServer(httpServerProperties);
            httpServer.start();

        } catch (Throwable t) {
            LOGGER.error("Cannot start application due to", t);
            shutdown();
            return;
        }

        Runtime.getRuntime().addShutdownHook(new Thread(ElasticAdapterApplication::shutdown));

        LOGGER.info("Elastic Adapter started for {} millis", System.currentTimeMillis() - start);
    }

    private static void shutdown() {
        long start = System.currentTimeMillis();

        LOGGER.info("Started Elastic Adapter shutdown");

        try {
            if (httpServer != null) {
                httpServer.stop(5_000, TimeUnit.MILLISECONDS);
            }
        } catch (Throwable t) {
            LOGGER.error("Error on http server shutdown", t);
        }

        try {
            if (gateSender != null) {
                gateSender.close();
            }
        } catch (Throwable t) {
            LOGGER.error("Error onn gate client serice shutdown", t);
        }

        try {
            if (metricsCollector != null) {
                metricsCollector.stop();
            }
        } catch (Throwable t) {
            LOGGER.error("Error on metrics collector shutdown", t);
        }

        LOGGER.info("Finished Elastic Adapter shutdown for {} millis", System.currentTimeMillis() - start);
    }

    private static HttpServer createHttpServer(Properties httpServerProperties) {
        RouteHandler handler = new InstrumentedRouteHandlerBuilder(httpServerProperties, metricsCollector).
                post("/_bulk", new BulkHandler(indexManager, gateSender)).
                post("/:index/_bulk", new BulkHandler(indexManager, gateSender)).
                post("/:index/_doc/_bulk", new BulkHandler(indexManager, gateSender)).
                post("/:index/_doc/", new IndexHandler(indexManager, gateSender)).
                build();

        return new UndertowHttpServer(
                Application.application().getConfig().getHost(),
                Application.application().getConfig().getPort(),
                httpServerProperties,
                handler);
    }

    private static class Props {
        static Parameter<String> LEPROSERY_STREAM =
                Parameter.stringParameter("leprosery.stream").
                        required().
                        build();

    }
}
