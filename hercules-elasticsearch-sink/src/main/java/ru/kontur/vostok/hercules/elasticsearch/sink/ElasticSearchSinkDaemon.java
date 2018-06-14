package ru.kontur.vostok.hercules.elasticsearch.sink;

import ru.kontur.vostok.hercules.meta.curator.CuratorClient;
import ru.kontur.vostok.hercules.meta.stream.DerivedStream;
import ru.kontur.vostok.hercules.meta.stream.Stream;
import ru.kontur.vostok.hercules.meta.stream.StreamRepository;
import ru.kontur.vostok.hercules.util.args.ArgsParser;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;

import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class ElasticSearchSinkDaemon {
    private static final Object lock = new Object();

    private static CuratorClient curatorClient;
    private static ElasticSearchSink elasticSearchSink;

    public static void main(String[] args) {
        long start = System.currentTimeMillis();

        Map<String, String> parameters = ArgsParser.parse(args);

        Properties streamProperties = PropertiesUtil.readProperties(parameters.getOrDefault("stream.properties", "stream.properties"));

        //TODO: Validate sinkProperties

        try {
            elasticSearchSink = new ElasticSearchSink(streamProperties);
            elasticSearchSink.start();
        } catch (Throwable e) {
            e.printStackTrace();
            shutdown();
            return;
        }

        Runtime.getRuntime().addShutdownHook(new Thread(ElasticSearchSinkDaemon::shutdown));

        System.out.println("Stream Sink Daemon started for " + (System.currentTimeMillis() - start) + " millis");
    }

    private static void shutdown() {
        long start = System.currentTimeMillis();
        System.out.println("Prepare Stream Sink Daemon to be shutdown");

        try {
            if (elasticSearchSink != null) {
                elasticSearchSink.stop(5_000, TimeUnit.MILLISECONDS);
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
