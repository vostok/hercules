package ru.kontur.vostok.hercules.cassandra.manager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.cassandra.util.CassandraConnector;
import ru.kontur.vostok.hercules.configuration.Scopes;
import ru.kontur.vostok.hercules.configuration.util.ArgsParser;
import ru.kontur.vostok.hercules.configuration.util.PropertiesReader;
import ru.kontur.vostok.hercules.configuration.util.PropertiesUtil;

import java.util.Map;
import java.util.Properties;

/**
 * @author Gregory Koshelev
 */
public class CassandraManagerApplication {

    private static final Logger LOGGER = LoggerFactory.getLogger(CassandraManagerApplication.class);

    private static CassandraConnector cassandraConnector;
    private static CassandraTaskConsumer consumer;

    public static void main(String[] args) {
        long start = System.currentTimeMillis();

        try {
            Map<String, String> parameters = ArgsParser.parse(args);

            Properties properties = PropertiesReader.read(parameters.getOrDefault("application.properties", "application.properties"));

            Properties cassandraProperties = PropertiesUtil.ofScope(properties, Scopes.CASSANDRA);
            cassandraConnector = new CassandraConnector(cassandraProperties);
            cassandraConnector.connect();

            Properties consumerProperties = PropertiesUtil.ofScope(properties, Scopes.CONSUMER);
            consumer = new CassandraTaskConsumer(consumerProperties, new CassandraManager(cassandraConnector));
            consumer.start();
        } catch (Throwable t) {
            LOGGER.error("Error on starting cassandra manager", t);
            shutdown();
            return;
        }

        Runtime.getRuntime().addShutdownHook(new Thread(CassandraManagerApplication::shutdown));

        LOGGER.info("Cassandra Manager started for {} millis", System.currentTimeMillis() - start);
    }

    private static void shutdown() {
        long start = System.currentTimeMillis();
        LOGGER.info("Started Cassandra Manager shutdown");

        try {
            if (consumer != null) {
                consumer.stop();
            }
        } catch (Throwable t) {
            LOGGER.error("Error on stopping consumer", t);
            //TODO: Process error
        }

        try {
            if (cassandraConnector != null) {
                cassandraConnector.close();
            }
        } catch (Throwable t) {
            LOGGER.error("Error on stopping cassandra manager", t);
            //TODO: Process error
        }

        LOGGER.info("Finished Cassandra Manager shutdown for {} millis", System.currentTimeMillis() - start);
    }
}
