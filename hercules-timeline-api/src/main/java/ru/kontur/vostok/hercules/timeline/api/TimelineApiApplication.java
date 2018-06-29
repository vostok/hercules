package ru.kontur.vostok.hercules.timeline.api;

import ru.kontur.vostok.hercules.auth.AuthManager;
import ru.kontur.vostok.hercules.cassandra.util.CassandraConnector;
import ru.kontur.vostok.hercules.meta.curator.CuratorClient;
import ru.kontur.vostok.hercules.meta.timeline.TimelineRepository;
import ru.kontur.vostok.hercules.util.args.ArgsParser;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;

import java.util.Map;
import java.util.Properties;

public class TimelineApiApplication {

    private static HttpServer server;
    private static CuratorClient curatorClient;
    private static TimelineReader timelineReader;
    private static CassandraConnector cassandraConnector;

    public static void main(String[] args) {
        long start = System.currentTimeMillis();

        try {
            Map<String, String> parameters = ArgsParser.parse(args);

            Properties httpServerProperties = PropertiesUtil.readProperties(parameters.getOrDefault("httpserver.properties", "httpserver.properties"));
            Properties curatorProperties = PropertiesUtil.readProperties(parameters.getOrDefault("curator.properties", "curator.properties"));
            Properties cassandraProperties = PropertiesUtil.readProperties(parameters.getOrDefault("cassandra.properties", "cassandra.properties"));

            curatorClient = new CuratorClient(curatorProperties);
            curatorClient.start();

            cassandraConnector = new CassandraConnector(cassandraProperties);
            cassandraConnector.connect();

            timelineReader = new TimelineReader(cassandraConnector);

            server = new HttpServer(
                    httpServerProperties,
                    new AuthManager(),
                    new ReadTimelineHandler(new TimelineRepository(curatorClient), timelineReader)
            );
            server.start();
        } catch (Throwable e) {
            e.printStackTrace();
            shutdown();
            return;
        }

        Runtime.getRuntime().addShutdownHook(new Thread(TimelineApiApplication::shutdown));

        System.out.println("Stream API started for " + (System.currentTimeMillis() - start) + " millis");
    }

    private static void shutdown() {
        long start = System.currentTimeMillis();
        System.out.println("Started Stream API shutdown");
        try {
            if (server != null) {
                server.stop();
            }
        } catch (Throwable e) {
            e.printStackTrace(); //TODO: Process error
        }
        try {
            if (curatorClient != null) {
                curatorClient.stop();
            }
        } catch (Throwable e) {
            e.printStackTrace(); //TODO: Process error
        }
        try {
            if (cassandraConnector != null) {
                cassandraConnector.close();
            }
        } catch (Throwable e) {
            e.printStackTrace(); //TODO: Process error
        }
        try {
            if (timelineReader != null) {
                timelineReader.shutdown();
            }
        } catch (Throwable e) {
            e.printStackTrace(); //TODO: Process error
        }
        System.out.println("Finished Stream API shutdown for " + (System.currentTimeMillis() - start) + " millis");
    }
}
