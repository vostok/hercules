package ru.kontur.vostok.hercules.cli;

import ru.kontur.vostok.hercules.meta.curator.CuratorClient;
import ru.kontur.vostok.hercules.util.args.ArgsParser;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;

import java.util.Map;
import java.util.Objects;
import java.util.Properties;

/**
 * Command line tool to initialize zookeeper nodes
 */
public class Initializer {

    private static final String[] ZOOKEEPER_NODES = new String[]  {
            "/hercules/auth/blacklist",
            "/hercules/auth/rules",
            "/hercules/stream",
            "/hercules/timeline"
    };

    private static CuratorClient curatorClient;

    public static void main(String[] args) throws Exception {
        Runtime.getRuntime().addShutdownHook(new Thread(Initializer::stop));

        Map<String, String> parameters = ArgsParser.parse(args);
        Properties properties = PropertiesUtil.readProperties(parameters.getOrDefault("initializer.properties", "initializer.properties"));

        curatorClient = new CuratorClient(PropertiesUtil.subProperties(properties, "curator"));
        curatorClient.start();

        for (String node : ZOOKEEPER_NODES) {
            curatorClient.createIfAbsent(node);
        }
    }

    private static void stop() {
        if (Objects.nonNull(curatorClient)) {
            curatorClient.stop();
        }
    }
}
