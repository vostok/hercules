package ru.kontur.vostok.hercules.management.api;

import ru.kontur.vostok.hercules.auth.AuthManager;
import ru.kontur.vostok.hercules.cassandra.util.CassandraConnector;
import ru.kontur.vostok.hercules.management.api.cassandra.CassandraManager;
import ru.kontur.vostok.hercules.management.api.kafka.KafkaManager;
import ru.kontur.vostok.hercules.meta.blacklist.BlacklistRepository;
import ru.kontur.vostok.hercules.meta.curator.CuratorClient;
import ru.kontur.vostok.hercules.meta.rule.RuleRepository;
import ru.kontur.vostok.hercules.meta.stream.StreamRepository;
import ru.kontur.vostok.hercules.meta.timeline.TimelineRepository;
import ru.kontur.vostok.hercules.util.args.ArgsParser;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * @author Gregory Koshelev
 */
public class ManagementApiApplication {
    private static HttpServer server;
    private static CuratorClient curatorClient;
    private static AuthManager authManager;
    private static CassandraConnector cassandraConnector;
    private static KafkaManager kafkaManager;

    public static void main(String[] args) {
        long start = System.currentTimeMillis();

        try {
            Map<String, String> parameters = ArgsParser.parse(args);

            Properties httpserverProperties = PropertiesUtil.readProperties(parameters.getOrDefault("httpserver.properties", "httpserver.properties"));
            Properties curatorProperties = PropertiesUtil.readProperties(parameters.getOrDefault("curator.properties", "curator.properties"));
            Properties applicationProperties = PropertiesUtil.readProperties(parameters.getOrDefault("application.properties", "application.properties"));
            Properties cassandraProperties = PropertiesUtil.readProperties(parameters.getOrDefault("cassandra.properties", "cassandra.properties"));
            Properties kafkaProperties = PropertiesUtil.readProperties(parameters.getOrDefault("kafka.properties", "kafka.properties"));

            curatorClient = new CuratorClient(curatorProperties);
            curatorClient.start();

            StreamRepository streamRepository = new StreamRepository(curatorClient);
            TimelineRepository timelineRepository = new TimelineRepository(curatorClient);
            BlacklistRepository blacklistRepository = new BlacklistRepository(curatorClient);
            RuleRepository ruleRepository = new RuleRepository(curatorClient);

            AdminManager adminManager = new AdminManager(PropertiesUtil.toSet(applicationProperties, "keys"));

            authManager = new AuthManager(curatorClient);
            authManager.start();

            cassandraConnector = new CassandraConnector(cassandraProperties);
            cassandraConnector.connect();
            CassandraManager cassandraManager = new CassandraManager(cassandraConnector);

            int replicationFactor = PropertiesUtil.get(applicationProperties, "replicationFactor", 1);

            kafkaManager = new KafkaManager(kafkaProperties, replicationFactor);

            server = new HttpServer(httpserverProperties, adminManager, authManager, streamRepository, timelineRepository, blacklistRepository, ruleRepository, cassandraManager, kafkaManager);
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
            if (cassandraConnector != null) {
                cassandraConnector.close();
            }
        } catch (Throwable e) {
            e.printStackTrace();//TODO: Process error
        }

        try {
            if (kafkaManager != null) {
                kafkaManager.stop(5000, TimeUnit.MILLISECONDS);
            }
        } catch (Throwable e) {
            e.printStackTrace();//TODO: Process error
        }

        System.out.println("Finished Management API shutdown for " + (System.currentTimeMillis() - start) + " millis");
    }
}
