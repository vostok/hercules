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
    private static ReadStreamHandler readStreamHandler;
    private static CuratorClient curatorClient;

    public static void main(String[] args) {
        long start = System.currentTimeMillis();

        try {
            Map<String, String> parameters = ArgsParser.parse(args);

            Properties httpServerProperties = PropertiesUtil.readProperties(parameters.getOrDefault("httpserver.properties", "httpserver.properties"));
            Properties curatorProperties = PropertiesUtil.readProperties(parameters.getOrDefault("curator.properties", "curator.properties"));

            curatorClient = new CuratorClient(curatorProperties);
            curatorClient.start();

            readStreamHandler = new ReadStreamHandler(new StreamReader(null, null, new StreamRepository(curatorClient)));

            StreamRepository streamRepository = new StreamRepository(curatorClient);
            AuthManager authManager = new AuthManager();

            server = new HttpServer(httpServerProperties, authManager, readStreamHandler, streamRepository);
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
            e.printStackTrace();//TODO: Process error
        }

        try {
            if (curatorClient != null) {
                curatorClient.stop();
            }
        } catch (Throwable e) {
            e.printStackTrace();//TODO: Process error
        }
        System.out.println("Finished Stream API shutdown for " + (System.currentTimeMillis() - start) + " millis");
    }
}
