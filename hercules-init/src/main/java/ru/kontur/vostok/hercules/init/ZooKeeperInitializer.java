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
            curatorClient.ensurePath("/hercules/streams");
            curatorClient.ensurePath("/hercules/timelines");
            curatorClient.ensurePath("/hercules/tasks/streams");
            curatorClient.ensurePath("/hercules/tasks/timelines");
            curatorClient.ensurePath("/hercules/auth/blacklist");
            curatorClient.ensurePath("/hercules/auth/rules");
            curatorClient.ensurePath("/hercules/auth/validations");
            curatorClient.ensurePath("/hercules/sd/services");
        } finally {
            curatorClient.stop();
        }
    }
}
