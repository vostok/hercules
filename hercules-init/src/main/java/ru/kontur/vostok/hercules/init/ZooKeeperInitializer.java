package ru.kontur.vostok.hercules.init;

import ru.kontur.vostok.hercules.curator.CuratorClient;

import java.util.Properties;

/**
 * @author Gregory Koshelev
 */
public class ZooKeeperInitializer {
    private final Properties curatorProperties;

    public ZooKeeperInitializer(Properties properties) {
        curatorProperties = properties;
    }

    public void init() throws Exception {
        CuratorClient curatorClient = new CuratorClient(curatorProperties);
        try {
            curatorClient.start();
            curatorClient.createIfAbsent("/hercules");
            curatorClient.createIfAbsent("/hercules/streams");
            curatorClient.createIfAbsent("/hercules/timelines");
            curatorClient.createIfAbsent("/hercules/tasks");
            curatorClient.createIfAbsent("/hercules/tasks/streams");
            curatorClient.createIfAbsent("/hercules/tasks/timelines");
            curatorClient.createIfAbsent("/hercules/auth");
            curatorClient.createIfAbsent("/hercules/auth/blacklist");
            curatorClient.createIfAbsent("/hercules/auth/rules");
            curatorClient.createIfAbsent("/hercules/auth/validations");
            curatorClient.createIfAbsent("/hercules/sink");
            curatorClient.createIfAbsent("/hercules/sink/sentry");
            curatorClient.createIfAbsent("/hercules/sink/sentry/registry");
        } finally {
            curatorClient.stop();
        }
    }
}
