package ru.kontur.vostok.hercules.stream.sink;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.application.Application;
import ru.kontur.vostok.hercules.configuration.PropertiesLoader;
import ru.kontur.vostok.hercules.configuration.Scopes;
import ru.kontur.vostok.hercules.configuration.util.ArgsParser;
import ru.kontur.vostok.hercules.curator.CuratorClient;
import ru.kontur.vostok.hercules.health.CommonMetrics;
import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.meta.stream.DerivedStream;
import ru.kontur.vostok.hercules.meta.stream.Stream;
import ru.kontur.vostok.hercules.meta.stream.StreamRepository;
import ru.kontur.vostok.hercules.undertow.util.servers.DaemonHttpServer;
import ru.kontur.vostok.hercules.util.application.ApplicationContextHolder;
import ru.kontur.vostok.hercules.util.parameter.Parameter;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * @author Gregory Koshelev
 */
public class StreamSinkDaemon {

    private static final Logger LOGGER = LoggerFactory.getLogger(StreamSinkDaemon.class);

    private static CuratorClient curatorClient;
    private static StreamSink streamSink;
    private static DaemonHttpServer daemonHttpServer;
    private static MetricsCollector metricsCollector;

    public static void main(String[] args) {
        long start = System.currentTimeMillis();

        Application.run("Hercules Stream Sink", "sink-stream", args);
        Map<String, String> parameters = ArgsParser.parse(args);

        Properties properties = PropertiesLoader.load(parameters.getOrDefault("application.properties", "file://application.properties"));

        Properties streamsProperties = PropertiesUtil.ofScope(properties, Scopes.STREAMS);
        Properties curatorProperties = PropertiesUtil.ofScope(properties, Scopes.CURATOR);
        Properties sinkProperties = PropertiesUtil.ofScope(properties, Scopes.SINK);
        Properties contextProperties = PropertiesUtil.ofScope(properties, Scopes.CONTEXT);
        Properties statusServerProperties = PropertiesUtil.ofScope(properties, Scopes.HTTP_SERVER);
        Properties metricsProperties = PropertiesUtil.ofScope(properties, Scopes.METRICS);

        ApplicationContextHolder.init("Hercules stream sink", "sink.stream", contextProperties);

        //TODO: Validate sinkProperties
        final String derivedName = PropertiesUtil.get(Props.DERIVED, sinkProperties).get();

        try {
            metricsCollector = new MetricsCollector(metricsProperties);
            metricsCollector.start();
            CommonMetrics.registerCommonMetrics(metricsCollector);

            daemonHttpServer = new DaemonHttpServer(statusServerProperties, metricsCollector);
            daemonHttpServer.start();

            curatorClient = new CuratorClient(curatorProperties);
            curatorClient.start();

            StreamRepository streamRepository = new StreamRepository(curatorClient);

            Optional<Stream> derivedOptional = streamRepository.read(derivedName);
            if (!derivedOptional.isPresent()) {
                throw new IllegalArgumentException("Unknown derived stream");
            }
            Stream derived = derivedOptional.get();
            if (!(derived instanceof DerivedStream)) {
                throw new IllegalArgumentException("Specified stream isn't derived one");
            }

            streamSink = new StreamSink(streamsProperties, (DerivedStream) derived);
            streamSink.start();
        } catch (Throwable t) {
            LOGGER.error("Error on starting stream sink daemon", t);
            shutdown();
            return;
        }

        Runtime.getRuntime().addShutdownHook(new Thread(StreamSinkDaemon::shutdown));

        LOGGER.info("Stream Sink Daemon started for {} millis", System.currentTimeMillis() - start);
    }

    private static void shutdown() {
        long start = System.currentTimeMillis();
        LOGGER.info("Prepare Stream Sink Daemon to be shutdown");

        try {
            if (streamSink != null) {
                streamSink.stop(5_000, TimeUnit.MILLISECONDS);
            }
        } catch (Throwable t) {
            LOGGER.error("Error on stopping stream sink", t);
            //TODO: Process error
        }

        try {
            if (curatorClient != null) {
                curatorClient.stop();
            }
        } catch (Throwable t) {
            LOGGER.error("Error on stopping curator client", t);
            //TODO: Process error
        }

        try {
            if (Objects.nonNull(daemonHttpServer)) {
                daemonHttpServer.stop(5_000, TimeUnit.MILLISECONDS);
            }
        } catch (Throwable t) {
            LOGGER.error("Error on stopping minimal status server", t);
            //TODO: Process error
        }

        try {
            if (metricsCollector != null) {
                metricsCollector.stop();
            }
        } catch (Throwable t) {
            LOGGER.error("Error on stopping metrics collector", t);
        }

        LOGGER.info("Finished Stream Sink Daemon shutdown for {} millis", System.currentTimeMillis() - start);
    }

    private static class Props {
        static final Parameter<String> DERIVED =
                Parameter.stringParameter("derived").
                        required().
                        build();
    }
}
