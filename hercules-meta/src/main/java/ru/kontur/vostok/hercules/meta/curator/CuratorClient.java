package ru.kontur.vostok.hercules.meta.curator;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.api.CuratorWatcher;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.util.properties.PropertyDescription;
import ru.kontur.vostok.hercules.util.properties.PropertyDescriptions;
import ru.kontur.vostok.hercules.util.validation.Validators;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * @author Gregory Koshelev
 */
public class CuratorClient {

    private static class Props {

        static final PropertyDescription<String> CONNECT_STRING = PropertyDescriptions
                .stringProperty("connectString")
                .withDefaultValue("localhost:2181")
                .build();

        static final PropertyDescription<Integer> CONNECTION_TIMEOUT_MS = PropertyDescriptions
                .integerProperty("connectionTimeout")
                .withDefaultValue(10_000)
                .withValidator(Validators.greaterOrEquals(0))
                .build();

        static final PropertyDescription<Integer> SESSION_TIMEOUT_MS = PropertyDescriptions
                .integerProperty("sessionTimeout")
                .withDefaultValue(30_000)
                .withValidator(Validators.greaterOrEquals(0))
                .build();

        static final PropertyDescription<Integer> BASE_SLEEP_TIME_MS = PropertyDescriptions
                .integerProperty("retryPolicy.baseSleepTime")
                .withDefaultValue(1_000)
                .withValidator(Validators.greaterOrEquals(0))
                .build();

        static final PropertyDescription<Integer> MAX_RETRIES = PropertyDescriptions
                .integerProperty("retryPolicy.maxRetries")
                .withDefaultValue(5)
                .withValidator(Validators.greaterOrEquals(0))
                .build();

        static final PropertyDescription<Integer> MAX_SLEEP_TIME_MS = PropertyDescriptions
                .integerProperty("retryPolicy.maxSleepTime")
                .withDefaultValue(8_000)
                .withValidator(Validators.greaterOrEquals(0))
                .build();
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(CuratorClient.class);

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
            Stat stat = curatorFramework.checkExists().forPath(path);
            if (stat == null) {
                return Optional.empty();
            }
            byte[] bytes = curatorFramework.getData().forPath(path);
            return bytes != null ? Optional.of(bytes) : Optional.empty();
        } catch (KeeperException.NoNodeException ex) {
            return Optional.empty();
        }
    }

    public List<String> children(String path) throws Exception {
        List<String> children = curatorFramework.getChildren().forPath(path);
        return children;
    }

    public List<String> children(String path, CuratorWatcher watcher) throws Exception {
        return curatorFramework.getChildren().usingWatcher(watcher).forPath(path);
    }

    public void createIfAbsent(String path) throws Exception {
        try {
            curatorFramework.create().forPath(path);
        } catch (KeeperException.NodeExistsException ex) {
            return; /* nothing to do: node already exists */
        }
    }

    public CreationResult create(String path, byte[] data) {
        return createWithMode(path, data, CreateMode.PERSISTENT);
    }

    public DeletionResult delete(String path) {
        try {
            curatorFramework.delete().forPath(path);
            return DeletionResult.ok();
        } catch (KeeperException.NoNodeException ex) {
            return DeletionResult.notExist();
        } catch (Exception ex) {
            LOGGER.error("Error on deleting path '" + path + "'", ex);
            return DeletionResult.unknown();
        }
    }

    public UpdateResult update(String path, byte[] data) {
        try {
            curatorFramework.setData().forPath(path, data);
            return UpdateResult.ok();
        } catch (KeeperException.NoNodeException ex) {
            return UpdateResult.notExist();
        } catch (Exception ex) {
            LOGGER.error("Error on updating path '" + path + "'", ex);
            return UpdateResult.unknown();
        }
    }

    public void createPath(String path) throws Exception {
        List<String> segments = Arrays.stream(path.split("/"))
                .filter(s -> Objects.nonNull(s) && !s.isEmpty())
                .collect(Collectors.toList());

        StringBuilder builder = new StringBuilder(path.length());
        for (String segment : segments) {
            builder.append('/').append(segment);
            String partialPath = builder.toString();
            Stat stat = curatorFramework.checkExists().forPath(partialPath);
            if (Objects.isNull(stat)) {
                curatorFramework.create().forPath(partialPath);
            }
        }
    }

    public CreationResult createWithMode(String path, byte[] data, CreateMode mode) {
        try {
            return CreationResult.ok(curatorFramework.create().withMode(mode).forPath(path, data));
        } catch (KeeperException.NodeExistsException ex) {
            return CreationResult.alreadyExist(path);
        } catch (Exception ex) {
            LOGGER.error("Error on creating path", ex);
            return CreationResult.unknown(path);
        }
    }

    private static CuratorFramework build(Properties properties) {
        final String connectString = Props.CONNECT_STRING.extract(properties);
        final int connectionTimeout = Props.CONNECTION_TIMEOUT_MS.extract(properties);
        final int sessionTimeout = Props.SESSION_TIMEOUT_MS.extract(properties);
        final int baseSleepTime = Props.BASE_SLEEP_TIME_MS.extract(properties);
        final int maxRetries = Props.MAX_RETRIES.extract(properties);
        final int maxSleepTime = Props.BASE_SLEEP_TIME_MS.extract(properties);

        ExponentialBackoffRetry retryPolicy = new ExponentialBackoffRetry(baseSleepTime, maxRetries, maxSleepTime);

        CuratorFramework curatorFramework = CuratorFrameworkFactory.builder()
                .zk34CompatibilityMode(true)
                .connectString(connectString)
                .connectionTimeoutMs(connectionTimeout)
                .sessionTimeoutMs(sessionTimeout)
                .retryPolicy(retryPolicy)
                .build();
        return curatorFramework;
    }
}
