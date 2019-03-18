package ru.kontur.vostok.hercules.init;

import ru.kontur.vostok.hercules.configuration.PropertiesLoader;
import ru.kontur.vostok.hercules.configuration.Scopes;
import ru.kontur.vostok.hercules.configuration.util.ArgsParser;
import ru.kontur.vostok.hercules.configuration.util.PropertiesUtil;
import ru.kontur.vostok.hercules.util.text.StringUtil;

import java.util.Map;
import java.util.Properties;

/**
 * @author Gregory Koshelev
 */
public class InitApplication {
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Arguments:\n"
                    + "\tapplication.properties=<path to file with properties>\n"
                    + "\tinit-zk=<true if needed>\n"
                    + "\tinit-kafka=<true if needed>\n"
                    + "\tinit-cassandra=<true if needed>");
            return;
        }

        Map<String, String> parameters = ArgsParser.parse(args);

        Properties properties =
                PropertiesLoader.load(parameters.getOrDefault("application.properties", "file://application.properties"));

        if (StringUtil.tryParseBoolean(parameters.get("init-zk"), false)) {
            try {
                new ZooKeeperInitializer(PropertiesUtil.ofScope(properties, Scopes.ZOOKEEPER)).init();
            } catch (Exception e) {
                System.out.println("ZooKeeper initialization fails with exception " + e.toString());
                e.printStackTrace();
            }
        }

        if (StringUtil.tryParseBoolean(parameters.get("init-kafka"), false)) {
            try {
                new KafkaInitializer(PropertiesUtil.ofScope(properties, Scopes.KAFKA)).init();
            } catch (Exception e) {
                System.out.println("Kafka initialization fails with exception " + e.toString());
                e.printStackTrace();
            }
        }

        if (StringUtil.tryParseBoolean(parameters.get("init-cassandra"), false)) {
            try {
                new CassandraInitializer(PropertiesUtil.ofScope(properties, Scopes.CASSANDRA)).init();
            } catch (Exception e) {
                System.out.println("Cassandra initialization fails with exception " + e.toString());
                e.printStackTrace();
            }
        }

        if (StringUtil.tryParseBoolean(parameters.get("init-tracing-cassandra"), false)) {
            try {
                new CassandraTracingInitializer(PropertiesUtil.ofScope(properties, "tracing.cassandra")).init();
            } catch (Exception e) {
                System.out.println("Cassandra initialization fails with exception " + e.toString());
                e.printStackTrace();
            }
        }
    }
}
