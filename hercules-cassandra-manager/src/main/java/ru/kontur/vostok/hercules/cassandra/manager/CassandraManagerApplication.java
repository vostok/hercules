package ru.kontur.vostok.hercules.cassandra.manager;

import ru.kontur.vostok.hercules.cassandra.util.CassandraConnector;
import ru.kontur.vostok.hercules.util.args.ArgsParser;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;

import java.util.Map;
import java.util.Properties;

/**
 * @author Gregory Koshelev
 */
public class CassandraManagerApplication {
    private static CassandraConnector cassandraConnector;
    private static CassandraTaskConsumer consumer;

    public static void main(String[] args) {
        long start = System.currentTimeMillis();

        try {
            Map<String, String> parameters = ArgsParser.parse(args);

            Properties cassandraProperties = PropertiesUtil.readProperties(parameters.getOrDefault("cassandra.properties", "cassandra.properties"));
            Properties consumerProperties = PropertiesUtil.readProperties(parameters.getOrDefault("consumer.properties", "consumer.properties"));

            cassandraConnector = new CassandraConnector(cassandraProperties);
            cassandraConnector.connect();

            consumer = new CassandraTaskConsumer(consumerProperties, new CassandraManager(cassandraConnector));
            consumer.start();
        } catch (Throwable e) {
            e.printStackTrace();
            shutdown();
            return;
        }

        Runtime.getRuntime().addShutdownHook(new Thread(CassandraManagerApplication::shutdown));

        System.out.println("Cassandra Manager started for " + (System.currentTimeMillis() - start) + " millis");
    }

    private static void shutdown() {
        long start = System.currentTimeMillis();
        System.out.println("Started Cassandra Manager shutdown");

        try {
            if (consumer != null) {
                consumer.stop();
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

        System.out.println("Finished Cassandra Manager shutdown for " + (System.currentTimeMillis() - start) + " millis");
    }
}
