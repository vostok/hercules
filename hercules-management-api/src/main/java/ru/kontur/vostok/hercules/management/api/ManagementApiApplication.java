package ru.kontur.vostok.hercules.management.api;

import ru.kontur.vostok.hercules.auth.AuthManager;
import ru.kontur.vostok.hercules.meta.curator.CuratorClient;
import ru.kontur.vostok.hercules.meta.stream.StreamRepository;
import ru.kontur.vostok.hercules.meta.timeline.TimelineRepository;
import ru.kontur.vostok.hercules.util.args.ArgsParser;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;

import java.util.Map;
import java.util.Properties;

/**
 * @author Gregory Koshelev
 */
public class ManagementApiApplication {
    private static HttpServer server;
    private static CuratorClient curatorClient;

    public static void main(String[] args) {
        long start = System.currentTimeMillis();

        try {
            Map<String, String> parameters = ArgsParser.parse(args);

            Properties httpserverProperties = PropertiesUtil.readProperties(parameters.getOrDefault("httpserver.properties", "httpserver.properties"));
            Properties curatorProperties = PropertiesUtil.readProperties(parameters.getOrDefault("curator.properties", "curator.properties"));

            curatorClient = new CuratorClient(curatorProperties);
            curatorClient.start();

            StreamRepository streamRepository = new StreamRepository(curatorClient);
            TimelineRepository timelineRepository = new TimelineRepository(curatorClient);

            AuthManager authManager = new AuthManager();

            server = new HttpServer(httpserverProperties, authManager, streamRepository, timelineRepository);
            server.start();
        } catch (Throwable e) {
            e.printStackTrace();
            shutdown();
            return;
        }

        Runtime.getRuntime().addShutdownHook(new Thread(ManagementApiApplication::shutdown));

        System.out.println("Management API started for " + (System.currentTimeMillis() - start) + " millis" );
    }

    private static void shutdown() {
        long start = System.currentTimeMillis();
        System.out.println("Started Management API shutdown");
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
        System.out.println("Finished Management API shutdown for " + (System.currentTimeMillis() - start) + " millis");
    }
}
