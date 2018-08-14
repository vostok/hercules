package ru.kontur.vostok.hercules.gateway;

import ru.kontur.vostok.hercules.auth.AuthManager;
import ru.kontur.vostok.hercules.meta.curator.CuratorClient;
import ru.kontur.vostok.hercules.meta.stream.StreamRepository;
import ru.kontur.vostok.hercules.metrics.MetricsCollector;
import ru.kontur.vostok.hercules.partitioner.HashPartitioner;
import ru.kontur.vostok.hercules.partitioner.NaiveHasher;
import ru.kontur.vostok.hercules.configuration.util.ArgsParser;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * @author Gregory Koshelev
 */
public class GatewayApplication {
    private static MetricsCollector metricsCollector;
    private static HttpServer server;
    private static EventSender eventSender;
    private static CuratorClient curatorClient;
    private static AuthManager authManager;

    public static void main(String[] args) {
        long start = System.currentTimeMillis();

        try {
            Map<String, String> parameters = ArgsParser.parse(args);

            Properties httpserverProperties = PropertiesUtil.readProperties(parameters.getOrDefault("httpserver.properties", "httpserver.properties"));
            Properties producerProperties = PropertiesUtil.readProperties(parameters.getOrDefault("producer.properties", "producer.properties"));
            Properties curatorProperties = PropertiesUtil.readProperties(parameters.getOrDefault("curator.properties", "curator.properties"));
            Properties metricsProperties = PropertiesUtil.readProperties(parameters.getOrDefault("metrics.properties", "metrics.properties"));

            metricsCollector = new MetricsCollector(metricsProperties);
            metricsCollector.start();

            eventSender = new EventSender(producerProperties, new HashPartitioner(new NaiveHasher()));

            curatorClient = new CuratorClient(curatorProperties);
            curatorClient.start();

            StreamRepository streamRepository = new StreamRepository(curatorClient);

            authManager = new AuthManager(curatorClient);
            authManager.start();

            server = new HttpServer(metricsCollector, httpserverProperties, authManager, eventSender, streamRepository);
            server.start();
        } catch (Throwable e) {
            e.printStackTrace();
            shutdown();
            return;
        }

        Runtime.getRuntime().addShutdownHook(new Thread(GatewayApplication::shutdown));

        System.out.println("Gateway started for " + (System.currentTimeMillis() - start) + " millis" );
    }

    private static void shutdown() {
        long start = System.currentTimeMillis();
        System.out.println("Started Gateway shutdown");
        try {
            if (server != null) {
                server.stop();
            }
        } catch (Throwable e) {
            e.printStackTrace();//TODO: Process error
        }

        try {
            if (eventSender != null) {
                eventSender.stop(5_000, TimeUnit.MILLISECONDS);
            }
        } catch (Throwable e) {
            e.printStackTrace();//TODO: Process error
        }

        try {
            if (authManager != null) {
                authManager.stop();
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }

        try {
            if (curatorClient != null) {
                curatorClient.stop();
            }
        } catch (Throwable e) {
            e.printStackTrace();//TODO: Process error
        }

        try {
            if (metricsCollector != null) {
                metricsCollector.stop();
            }
        } catch (Throwable e) {
            e.printStackTrace();//TODO: Process error
        }

        System.out.println("Finished Gateway shutdown for " + (System.currentTimeMillis() - start) + " millis");
    }
}
