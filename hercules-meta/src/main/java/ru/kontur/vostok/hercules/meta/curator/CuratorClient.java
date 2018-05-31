package ru.kontur.vostok.hercules.meta.curator;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.KeeperException;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;

import java.util.Optional;
import java.util.Properties;

/**
 * @author Gregory Koshelev
 */
public class CuratorClient {
    private final CuratorFramework curatorFramework;

    public CuratorClient(Properties properties) {
        this.curatorFramework = build(properties);
    }

    public void start() {
        curatorFramework.start();
    }

    public void stop() {
        curatorFramework.close();
    }

    public Optional<byte[]> read(String path) throws Exception {
        try {
            byte[] bytes = curatorFramework.getData().forPath(path);
            return bytes != null ? Optional.of(bytes) : Optional.empty();
        } catch (KeeperException.NoNodeException ex) {
            return Optional.empty();
        }
    }

    private static CuratorFramework build(Properties properties) {
        String connectString = properties.getProperty("connectString", "localhost:2181");
        int connectionTimeout = PropertiesUtil.get(properties, "connectionTimeout", 10_000);
        int sessionTimeout = PropertiesUtil.get(properties, "sessionTimeout", 30_000);
        int baseSleepTime = PropertiesUtil.get(properties, "retryPolicy.baseSleepTime", 1_000);
        int maxRetries = PropertiesUtil.get(properties, "retryPolicy.maxRetries", 5);
        int maxSleepTime = PropertiesUtil.get(properties, "retryPolicy.maxSleepTime", 8_000);

        ExponentialBackoffRetry retryPolicy = new ExponentialBackoffRetry(baseSleepTime, maxRetries, maxSleepTime);

        CuratorFramework curatorFramework = CuratorFrameworkFactory.builder()
                .connectString(connectString)
                .connectionTimeoutMs(connectionTimeout)
                .sessionTimeoutMs(sessionTimeout)
                .retryPolicy(retryPolicy)
                .build();
        return curatorFramework;
    }
}
