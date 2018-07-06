package ru.kontur.vostok.hercules.graphite.sink;

import ru.kontur.vostok.hercules.meta.curator.CuratorClient;
import ru.kontur.vostok.hercules.meta.stream.Stream;
import ru.kontur.vostok.hercules.meta.stream.StreamRepository;
import ru.kontur.vostok.hercules.util.args.ArgsParser;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;

import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static ru.kontur.vostok.hercules.util.throwable.ThrowableUtil.toUnchecked;

public class GraphiteSinkDaemon {

    private static CuratorClient curatorClient;
    private static GraphiteSink graphiteSink;

    public static void main(String[] args) {
        long start = System.currentTimeMillis();

        Map<String, String> parameters = ArgsParser.parse(args);

        Properties curatorProperties = PropertiesUtil.readProperties(parameters.getOrDefault("curator.properties", "curator.properties"));
        Properties streamProperties = PropertiesUtil.readProperties(parameters.getOrDefault("streams.properties", "streams.properties"));
        Properties graphiteProperties = PropertiesUtil.readProperties(parameters.getOrDefault("graphite.properties", "graphite.properties"));

        curatorClient = new CuratorClient(curatorProperties);
        curatorClient.start();

        StreamRepository streamRepository = new StreamRepository(curatorClient);

        String streamName = streamProperties.getProperty("stream.name");
        Optional<Stream> stream = toUnchecked(() -> streamRepository.read(streamName));
        if (!stream.isPresent()) {
            throw new IllegalArgumentException("Unknown stream");
        }

        //TODO: Validate sinkProperties
        try {
            graphiteSink = new GraphiteSink(stream.get(), streamProperties, new GraphiteEventSender(graphiteProperties));
            graphiteSink.start();
        } catch (Throwable e) {
            e.printStackTrace();
            shutdown();
            return;
        }

        Runtime.getRuntime().addShutdownHook(new Thread(GraphiteSinkDaemon::shutdown));

        System.out.println("Graphite Sink Daemon started for " + (System.currentTimeMillis() - start) + " millis");
    }

    private static void shutdown() {
        long start = System.currentTimeMillis();
        System.out.println("Prepare Graphite Sink Daemon to be shutdown");

        try {
            if (graphiteSink != null) {
                graphiteSink.stop(5_000, TimeUnit.MILLISECONDS);
            }
        } catch (Throwable e) {
            e.printStackTrace();//TODO: Process error
        }

        try {
            if (curatorClient != null) {
                curatorClient.stop();
            }
        } catch (Throwable e) {
            e.printStackTrace();//TODO: Process error
        }

        System.out.println("Finished Elasticsearch Sink Daemon shutdown for " + (System.currentTimeMillis() - start) + " millis");
    }
}
