package ru.kontur.vostok.hercules.init;

import org.apache.curator.framework.CuratorFramework;
import ru.kontur.vostok.hercules.meta.curator.CuratorClient;

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
            curatorClient.createIfAbsent("/hercules/auth");
            curatorClient.createIfAbsent("/hercules/auth/blacklist");
            curatorClient.createIfAbsent("/hercules/auth/rules");
            curatorClient.createIfAbsent("/hercules/auth/validations");
        } finally {
            curatorClient.stop();
        }
    }
}
