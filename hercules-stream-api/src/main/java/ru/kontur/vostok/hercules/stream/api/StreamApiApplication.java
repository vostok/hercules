package ru.kontur.vostok.hercules.stream.api;

import ru.kontur.vostok.hercules.auth.AuthManager;
import ru.kontur.vostok.hercules.meta.curator.CuratorClient;
import ru.kontur.vostok.hercules.meta.stream.StreamRepository;
import ru.kontur.vostok.hercules.util.args.ArgsParser;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;

import java.util.Map;
import java.util.Properties;


public class StreamApiApplication {

    private static HttpServer server;
    private static CuratorClient curatorClient;
    private static StreamReader streamReader;
    private static AuthManager authManager;

    public static void main(String[] args) {
        long start = System.currentTimeMillis();

        try {
            Map<String, String> parameters = ArgsParser.parse(args);

            Properties httpServerProperties = PropertiesUtil.readProperties(parameters.getOrDefault("httpserver.properties", "httpserver.properties"));
            Properties curatorProperties = PropertiesUtil.readProperties(parameters.getOrDefault("curator.properties", "curator.properties"));
            Properties consumerProperties = PropertiesUtil.readProperties(parameters.getOrDefault("consumer.properties", "consumer.properties"));

            curatorClient = new CuratorClient(curatorProperties);
            curatorClient.start();

            streamReader = new StreamReader(consumerProperties, new StreamRepository(curatorClient));

            authManager = new AuthManager(curatorClient);

            server = new HttpServer(httpServerProperties, authManager, new ReadStreamHandler(streamReader));
            server.start();
        } catch (Throwable e) {
            e.printStackTrace();
            shutdown();
            return;
        }

        Runtime.getRuntime().addShutdownHook(new Thread(StreamApiApplication::shutdown));

        System.out.println("Stream API started for " + (System.currentTimeMillis() - start) + " millis" );
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
            if (authManager != null) {
                authManager.stop();
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }

        try {
            if (streamReader != null) {
                streamReader.stop();
            }
        }
        catch (Throwable e) {
            e.printStackTrace(); //TODO: Process error
        }

        System.out.println("Finished Stream API shutdown for " + (System.currentTimeMillis() - start) + " millis");
    }
}
