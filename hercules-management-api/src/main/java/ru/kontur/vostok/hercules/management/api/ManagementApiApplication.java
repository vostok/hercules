package ru.kontur.vostok.hercules.management.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.auth.AdminAuthManager;
import ru.kontur.vostok.hercules.auth.AuthManager;
import ru.kontur.vostok.hercules.configuration.Scopes;
import ru.kontur.vostok.hercules.configuration.util.ArgsParser;
import ru.kontur.vostok.hercules.configuration.util.PropertiesReader;
import ru.kontur.vostok.hercules.configuration.util.PropertiesUtil;
import ru.kontur.vostok.hercules.management.api.task.CassandraTaskQueue;
import ru.kontur.vostok.hercules.management.api.task.KafkaTaskQueue;
import ru.kontur.vostok.hercules.meta.auth.blacklist.BlacklistRepository;
import ru.kontur.vostok.hercules.meta.curator.CuratorClient;
import ru.kontur.vostok.hercules.meta.auth.rule.RuleRepository;
import ru.kontur.vostok.hercules.meta.sink.sentry.SentryProjectRepository;
import ru.kontur.vostok.hercules.meta.stream.StreamRepository;
import ru.kontur.vostok.hercules.meta.timeline.TimelineRepository;
import ru.kontur.vostok.hercules.util.properties.PropertiesExtractor;
import ru.kontur.vostok.hercules.util.properties.PropertyDescription;
import ru.kontur.vostok.hercules.util.properties.PropertyDescriptions;

import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * @author Gregory Koshelev
 */
public class ManagementApiApplication {

    private static class Props {
        static final PropertyDescription<Set<String>> ADMIN_KEYS = PropertyDescriptions
                .setOfStringsProperty("keys")
                .build();
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(ManagementApiApplication.class);

    private static HttpServer server;
    private static CuratorClient curatorClient;
    private static AuthManager authManager;
    private static CassandraTaskQueue cassandraTaskQueue;
    private static KafkaTaskQueue kafkaTaskQueue;

    public static void main(String[] args) {
        long start = System.currentTimeMillis();

        try {
            Map<String, String> parameters = ArgsParser.parse(args);

            Properties properties = PropertiesReader.read(parameters.getOrDefault("application.properties", "application.properties"));

            Properties httpserverProperties = PropertiesUtil.ofScope(properties, Scopes.HTTP_SERVER);
            Properties curatorProperties = PropertiesUtil.ofScope(properties, Scopes.CURATOR);
            Properties kafkaProperties = PropertiesUtil.ofScope(properties, Scopes.KAFKA);

            curatorClient = new CuratorClient(curatorProperties);
            curatorClient.start();

            StreamRepository streamRepository = new StreamRepository(curatorClient);
            TimelineRepository timelineRepository = new TimelineRepository(curatorClient);
            BlacklistRepository blacklistRepository = new BlacklistRepository(curatorClient);
            RuleRepository ruleRepository = new RuleRepository(curatorClient);
            SentryProjectRepository sentryProjectRepository = new SentryProjectRepository(curatorClient);

            AdminAuthManager adminAuthManager = new AdminAuthManager(Props.ADMIN_KEYS.extract(properties));

            authManager = new AuthManager(curatorClient);
            authManager.start();

            cassandraTaskQueue = new CassandraTaskQueue(kafkaProperties);
            kafkaTaskQueue = new KafkaTaskQueue(kafkaProperties);

            server = new HttpServer(
                    httpserverProperties,
                    adminAuthManager,
                    authManager,
                    streamRepository,
                    timelineRepository,
                    blacklistRepository,
                    ruleRepository,
                    sentryProjectRepository,
                    cassandraTaskQueue,
                    kafkaTaskQueue
            );
            server.start();
        } catch (Throwable e) {
            LOGGER.error("Error on starting management api", e);
            shutdown();
            return;
        }

        Runtime.getRuntime().addShutdownHook(new Thread(ManagementApiApplication::shutdown));

        LOGGER.info("Management API started for {} millis", System.currentTimeMillis() - start);
    }

    private static void shutdown() {
        long start = System.currentTimeMillis();
        LOGGER.info("Started Management API shutdown");
        try {
            if (server != null) {
                server.stop();
            }
        } catch (Throwable e) {
            LOGGER.error("Error on server stopping", e);
            //TODO: Process error
        }

        try {
            if (authManager != null) {
                authManager.stop();
            }
        } catch (Throwable e) {
            LOGGER.error("Error on auth manager stopping", e);
        }

        try {
            if (curatorClient != null) {
                curatorClient.stop();
            }
        } catch (Throwable e) {
            LOGGER.error("Error on curator client stopping", e);
            //TODO: Process error
        }

        try {
            if (cassandraTaskQueue != null) {
                cassandraTaskQueue.close(5_000, TimeUnit.MILLISECONDS);
            }
        } catch (Throwable e) {
            LOGGER.error("Error on cassandra task queue stopping", e);
            //TODO: Process error
        }

        try {
            if (kafkaTaskQueue != null) {
                kafkaTaskQueue.close(5_000, TimeUnit.MILLISECONDS);
            }
        } catch (Throwable e) {
            LOGGER.error("Error on kafka task queue stopping", e);
            //TODO: Process error
        }

        LOGGER.info("Finished Management API shutdown for {} millis", System.currentTimeMillis() - start);
    }
}
