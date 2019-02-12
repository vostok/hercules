package ru.kontur.vostok.hercules.timeline.manager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.cassandra.util.CassandraConnector;
import ru.kontur.vostok.hercules.configuration.Scopes;
import ru.kontur.vostok.hercules.configuration.util.ArgsParser;
import ru.kontur.vostok.hercules.configuration.util.PropertiesReader;
import ru.kontur.vostok.hercules.configuration.util.PropertiesUtil;
import ru.kontur.vostok.hercules.meta.curator.CuratorClient;
import ru.kontur.vostok.hercules.meta.task.timeline.TimelineTaskRepository;
import ru.kontur.vostok.hercules.meta.timeline.TimelineRepository;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * @author Gregory Koshelev
 */
public class TimelineManagerApplication {

    private static final Logger LOGGER = LoggerFactory.getLogger(TimelineManagerApplication.class);

    private static CassandraConnector cassandraConnector;
    private static CuratorClient curatorClient;
    private static TimelineTaskExecutor timelineTaskExecutor;

    public static void main(String[] args) {
        long start = System.currentTimeMillis();

        try {
            Map<String, String> parameters = ArgsParser.parse(args);

            Properties properties = PropertiesReader.read(parameters.getOrDefault("application.properties", "application.properties"));
            Properties cassandraProperties = PropertiesUtil.ofScope(properties, Scopes.CASSANDRA);
            Properties curatorProperties = PropertiesUtil.ofScope(properties, Scopes.CURATOR);

            cassandraConnector = new CassandraConnector(cassandraProperties);
            cassandraConnector.connect();

            CassandraManager cassandraManager = new CassandraManager(cassandraConnector);

            curatorClient = new CuratorClient(curatorProperties);
            curatorClient.start();

            TimelineTaskRepository timelineTaskRepository = new TimelineTaskRepository(curatorClient);
            TimelineRepository timelineRepository = new TimelineRepository(curatorClient);

            timelineTaskExecutor =
                    new TimelineTaskExecutor(timelineTaskRepository, 5_000, cassandraManager, timelineRepository);
            timelineTaskExecutor.start();
        } catch (Throwable t) {
            LOGGER.error("Error on starting Timeline Manager", t);
            shutdown();
            return;
        }

        Runtime.getRuntime().addShutdownHook(new Thread(TimelineManagerApplication::shutdown));

        LOGGER.info("Timeline Manager started for {} millis", System.currentTimeMillis() - start);
    }

    private static void shutdown() {
        long start = System.currentTimeMillis();
        LOGGER.info("Started Timeline Manager shutdown");

        try {
            if (timelineTaskExecutor != null) {
                timelineTaskExecutor.stop(5_000, TimeUnit.MILLISECONDS);
            }
        } catch (Throwable t) {
            LOGGER.error("Error on stopping task executor", t);
            //TODO: Process error
        }

        try {
            if (curatorClient != null) {
                curatorClient.stop();
            }
        } catch (Throwable t) {
            LOGGER.error("Error on stopping curator", t);
            //TODO: Process error
        }

        try {
            if (cassandraConnector != null) {
                cassandraConnector.close();
            }
        } catch (Throwable t) {
            LOGGER.error("Error on stopping cassandra connector", t);
            //TODO: Process error
        }

        LOGGER.info("Finished Timeline Manager shutdown for {} millis", System.currentTimeMillis() - start);
    }
}
