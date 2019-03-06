package ru.kontur.vostok.hercules.curator;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.api.CuratorWatcher;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.curator.exception.CuratorInternalException;
import ru.kontur.vostok.hercules.curator.exception.CuratorUnknownException;
import ru.kontur.vostok.hercules.curator.result.CreationResult;
import ru.kontur.vostok.hercules.curator.result.DeletionResult;
import ru.kontur.vostok.hercules.curator.result.ReadResult;
import ru.kontur.vostok.hercules.curator.result.UpdateResult;
import ru.kontur.vostok.hercules.util.properties.PropertyDescription;
import ru.kontur.vostok.hercules.util.properties.PropertyDescriptions;
import ru.kontur.vostok.hercules.util.validation.Validators;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * @author Gregory Koshelev
 */
public class CuratorClient {
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

    public ReadResult read(String path) throws CuratorInternalException, CuratorUnknownException {
        try {
            Stat stat = curatorFramework.checkExists().forPath(path);
            if (stat == null) {
                return ReadResult.notFound();
            }
            byte[] bytes = curatorFramework.getData().forPath(path);
            return ReadResult.found(bytes);
        } catch (KeeperException.NoNodeException ex) {
            return ReadResult.notFound();
        } catch (KeeperException ex) {
            throw new CuratorInternalException("Read failed with KeeperException", ex);
        } catch (Exception ex) {
            throw new CuratorUnknownException("Read failed with Exception", ex);
        }
    }

    /**
     * @param path path
     * @return unordered list of children
     */
    public List<String> children(String path) throws Exception {
        return curatorFramework.getChildren().forPath(path);
    }

    /**
     * @param path    path
     * @param watcher watcher
     * @return unordered list of children
     */
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

    public CreationResult create(String path, byte[] data) throws CuratorUnknownException, CuratorInternalException {
        return createWithMode(path, data, CreateMode.PERSISTENT);
    }

    public DeletionResult delete(String path) throws CuratorInternalException, CuratorUnknownException {
        try {
            curatorFramework.delete().forPath(path);
            return DeletionResult.ok();
        } catch (KeeperException.NoNodeException ex) {
            return DeletionResult.notExist();
        } catch (KeeperException ex) {
            throw new CuratorInternalException("Delete failed with KeeperException", ex);
        } catch (Exception ex) {
            throw new CuratorUnknownException("Delete failed with Exception", ex);
        }
    }

    public UpdateResult update(String path, byte[] data) throws CuratorInternalException, CuratorUnknownException {
        try {
            curatorFramework.setData().forPath(path, data);
            return UpdateResult.ok();
        } catch (KeeperException.NoNodeException ex) {
            return UpdateResult.notExist();
        } catch (KeeperException ex) {
            throw new CuratorInternalException("Update failed with KeeperException", ex);
        } catch (Exception ex) {
            throw new CuratorUnknownException("Update failed with Exception", ex);
        }
    }

    /**
     * Check node existence
     *
     * @param path path of the node
     * @return {@code true} if node exists, {@code false} otherwise
     * @throws CuratorUnknownException
     */
    public boolean exists(String path) throws CuratorUnknownException {
        try {
            return curatorFramework.checkExists().forPath(path) != null;
        } catch (Exception ex) {
            throw new CuratorUnknownException("Existence check failed with Exception", ex);
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

    public CreationResult createWithMode(String path, byte[] data, CreateMode mode)
            throws CuratorInternalException, CuratorUnknownException {
        try {
            return CreationResult.ok(curatorFramework.create().withMode(mode).forPath(path, data));
        } catch (KeeperException.NodeExistsException ex) {
            return CreationResult.alreadyExist(path);
        } catch (KeeperException ex) {
            throw new CuratorInternalException("Create failed with KeeperException", ex);
        } catch (Exception ex) {
            throw new CuratorUnknownException("Create failed with Exception", ex);
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
}
