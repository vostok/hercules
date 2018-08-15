package ru.kontur.vostok.hercules.stream.api;

import ru.kontur.vostok.hercules.auth.AuthManager;
import ru.kontur.vostok.hercules.configuration.Scopes;
import ru.kontur.vostok.hercules.configuration.util.ArgsParser;
import ru.kontur.vostok.hercules.configuration.util.PropertiesReader;
import ru.kontur.vostok.hercules.configuration.util.PropertiesUtil;
import ru.kontur.vostok.hercules.meta.curator.CuratorClient;
import ru.kontur.vostok.hercules.meta.stream.StreamRepository;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;


public class StreamApiApplication {

    private static HttpServer server;
    private static CuratorClient curatorClient;
    private static StreamReader streamReader;
    private static AuthManager authManager;

    public static void main(String[] args) {
        long start = System.currentTimeMillis();

        try {
            Map<String, String> parameters = ArgsParser.parse(args);

            Properties properties = PropertiesReader.read(parameters.getOrDefault("application.properties", "application.properties"));

            Properties httpServerProperties = PropertiesUtil.ofScope(properties, Scopes.HTTP_SERVER);
            Properties curatorProperties = PropertiesUtil.ofScope(properties, Scopes.CURATOR);
            Properties consumerProperties = PropertiesUtil.ofScope(properties, Scopes.CONSUMER);

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
                curatorClient.stop(5_000, TimeUnit.MILLISECONDS);
            }
        } catch (Throwable e) {
            e.printStackTrace(); //TODO: Process error
        }

        try {
            if (authManager != null) {
                authManager.stop(5_000, TimeUnit.MILLISECONDS);
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
