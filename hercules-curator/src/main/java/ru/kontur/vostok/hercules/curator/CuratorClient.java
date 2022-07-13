package ru.kontur.vostok.hercules.curator;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.api.BackgroundPathable;
import org.apache.curator.framework.api.CuratorWatcher;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.curator.exception.CuratorConnectionException;
import ru.kontur.vostok.hercules.curator.exception.CuratorInternalException;
import ru.kontur.vostok.hercules.curator.exception.CuratorUnknownException;
import ru.kontur.vostok.hercules.curator.result.CreationResult;
import ru.kontur.vostok.hercules.curator.result.DeletionResult;
import ru.kontur.vostok.hercules.curator.result.ReadResult;
import ru.kontur.vostok.hercules.curator.result.UpdateResult;
import ru.kontur.vostok.hercules.util.lifecycle.Lifecycle;
import ru.kontur.vostok.hercules.util.parameter.Parameter;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;
import ru.kontur.vostok.hercules.util.validation.IntegerValidators;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author Gregory Koshelev
 */
public class CuratorClient implements Lifecycle {
    private static final Logger LOGGER = LoggerFactory.getLogger(CuratorClient.class);

    private final CuratorFramework curatorFramework;

    public CuratorClient(Properties properties) {
        this.curatorFramework = build(properties);
    }

    @Override
    public void start() {
        curatorFramework.start();
    }

    /**
     * @deprecated use {@link #stop(long, TimeUnit)} instead
     */
    @Deprecated
    public void stop() {
        stop(0, TimeUnit.MILLISECONDS);
    }

    @Override
    public boolean stop(long timeout, TimeUnit unit) {
        curatorFramework.close();
        return true;
    }

    public ReadResult read(String path) throws CuratorInternalException, CuratorUnknownException {
        return readImpl(path, curatorFramework.getData());
    }

    public ReadResult read(String path, CuratorWatcher curatorWatcher)
            throws CuratorInternalException, CuratorUnknownException {
        return readImpl(path, curatorFramework.getData().usingWatcher(curatorWatcher));
    }

    public void registerConnectionStateListener(ConnectionStateListener listener) {
        curatorFramework.getConnectionStateListenable().addListener(listener);
    }

    public void registerConnectionStateListener(ConnectionStateListener listener, Executor executor) {
        curatorFramework.getConnectionStateListenable().addListener(listener, executor);
    }

    public void removeConnectionStateListener(ConnectionStateListener listener) {
        curatorFramework.getConnectionStateListenable().removeListener(listener);
    }

    /**
     * @param path path
     * @return unordered list of children
     */
    public List<String> children(String path) throws CuratorInternalException, CuratorUnknownException {
        try {
            return curatorFramework.getChildren().forPath(path);
        } catch (KeeperException ex) {
            throw new CuratorInternalException("Get children failed with KeeperException", ex);
        } catch (Exception ex) {
            throw new CuratorUnknownException("Get children failed with Exception", ex);
        }
    }

    /**
     * @param path    path
     * @param watcher watcher
     * @return unordered list of children
     */
    public List<String> children(String path, CuratorWatcher watcher) throws Exception {
        return curatorFramework.getChildren().usingWatcher(watcher).forPath(path);
    }

    public void createIfAbsent(String path) throws CuratorUnknownException, CuratorInternalException {
        try {
            curatorFramework.create().forPath(path);
        } catch (KeeperException.NodeExistsException ex) {
            return; /* nothing to do: node already exists */
        } catch (KeeperException ex) {
            throw new CuratorInternalException("Create node failed with KeeperException", ex);
        } catch (Exception ex) {
            throw new CuratorUnknownException("Create node failed with Exception", ex);
        }
    }

    public void ensurePath(String path) throws Exception {
        try {
            curatorFramework.create().creatingParentsIfNeeded().forPath(path);
        } catch (KeeperException.NodeExistsException ex) {
            return; /* nothing to do: node alredy exists */
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
     * @throws CuratorUnknownException Will be thrown as a result of any error.
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
            return CreationResult.ok(curatorFramework.create().creatingParentsIfNeeded().withMode(mode).forPath(path, data));
        } catch (KeeperException.NodeExistsException ex) {
            return CreationResult.alreadyExist(path);
        } catch (KeeperException ex) {
            throw new CuratorInternalException("Create failed with KeeperException", ex);
        } catch (Exception ex) {
            throw new CuratorUnknownException("Create failed with Exception", ex);
        }
    }

    public long getSessionId() throws CuratorConnectionException {
        verify();

        try {
            return curatorFramework.getZookeeperClient().getZooKeeper().getSessionId();
        } catch (Exception ex) {
            throw new CuratorConnectionException(ex);
        }
    }

    public CuratorFramework getCuratorFramework() {
        verify();

        return curatorFramework;
    }

    private void verify() {
        if (curatorFramework == null) {
            throw new IllegalStateException("CuratorFramework is null");
        }
        final CuratorFrameworkState state = curatorFramework.getState();
        if (state != CuratorFrameworkState.STARTED) {
            throw new IllegalStateException("CuratorFramework state is " + state);
        }
    }

    private ReadResult readImpl(String path, BackgroundPathable<byte[]> dataPathable)
            throws CuratorInternalException, CuratorUnknownException {
        try {
            Stat stat = this.curatorFramework.checkExists().forPath(path);
            if (stat == null) {
                return ReadResult.notFound();
            }
            byte[] bytes = dataPathable.forPath(path);
            return ReadResult.found(bytes);
        } catch (KeeperException.NoNodeException ex) {
            return ReadResult.notFound();
        } catch (KeeperException ex) {
            throw new CuratorInternalException("Read failed with KeeperException", ex);
        } catch (Exception ex) {
            throw new CuratorUnknownException("Read failed with Exception", ex);
        }
    }

    private static CuratorFramework build(Properties properties) {
        final String connectString = PropertiesUtil.get(Props.CONNECT_STRING, properties).get();
        final int connectionTimeout = PropertiesUtil.get(Props.CONNECTION_TIMEOUT_MS, properties).get();
        final int sessionTimeout = PropertiesUtil.get(Props.SESSION_TIMEOUT_MS, properties).get();
        final int baseSleepTime = PropertiesUtil.get(Props.BASE_SLEEP_TIME_MS, properties).get();
        final int maxRetries = PropertiesUtil.get(Props.MAX_RETRIES, properties).get();
        final int maxSleepTime = PropertiesUtil.get(Props.MAX_SLEEP_TIME_MS, properties).get();

        ExponentialBackoffRetry retryPolicy = new ExponentialBackoffRetry(baseSleepTime, maxRetries, maxSleepTime);

        return CuratorFrameworkFactory.builder()
                .zk34CompatibilityMode(true)
                .connectString(connectString)
                .connectionTimeoutMs(connectionTimeout)
                .sessionTimeoutMs(sessionTimeout)
                .retryPolicy(retryPolicy)
                .build();
    }

    private static class Props {
        static final Parameter<String> CONNECT_STRING =
                Parameter.stringParameter("connectString").
                        withDefault("localhost:2181").
                        build();

        static final Parameter<Integer> CONNECTION_TIMEOUT_MS =
                Parameter.integerParameter("connectionTimeout").
                        withDefault(10_000).
                        withValidator(IntegerValidators.nonNegative()).
                        build();

        static final Parameter<Integer> SESSION_TIMEOUT_MS =
                Parameter.integerParameter("sessionTimeout").
                        withDefault(30_000).
                        withValidator(IntegerValidators.nonNegative()).
                        build();

        static final Parameter<Integer> BASE_SLEEP_TIME_MS =
                Parameter.integerParameter("retryPolicy.baseSleepTime").
                        withDefault(1_000).
                        withValidator(IntegerValidators.nonNegative()).
                        build();

        static final Parameter<Integer> MAX_RETRIES =
                Parameter.integerParameter("retryPolicy.maxRetries").
                        withDefault(5).
                        withValidator(IntegerValidators.nonNegative()).
                        build();

        static final Parameter<Integer> MAX_SLEEP_TIME_MS =
                Parameter.integerParameter("retryPolicy.maxSleepTime").
                        withDefault(8_000).
                        withValidator(IntegerValidators.nonNegative()).
                        build();
    }
}
