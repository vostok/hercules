package ru.kontur.vostok.hercules.routing.config.zk;

import org.apache.curator.framework.api.CuratorWatcher;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.curator.CuratorClient;
import ru.kontur.vostok.hercules.curator.result.ReadResult;
import ru.kontur.vostok.hercules.routing.engine.EngineConfig;
import ru.kontur.vostok.hercules.routing.engine.EngineConfigDeserializer;
import ru.kontur.vostok.hercules.routing.engine.Route;
import ru.kontur.vostok.hercules.routing.engine.RouteDeserializer;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Executor;

/**
 * Repository for reading routes and engines configs stored in ZooKeeper.
 *
 * @author Aleksandr Yuferov
 */
public class ZookeeperReadRepository {
    private static final Logger LOGGER = LoggerFactory.getLogger(ZookeeperReadRepository.class);
    private final CuratorClient curatorClient;
    private final String rootPath;
    private final RouteDeserializer routeDeserializer;
    private final EngineConfigDeserializer configDeserializer;

    /**
     * Builder factory.
     *
     * @return Builder of repository.
     */
    public static Builder builder() {
        return new Builder();
    }

    protected ZookeeperReadRepository(Builder builder) {
        this.curatorClient = builder.curatorClient;
        this.rootPath = builder.rootPath;
        this.routeDeserializer = builder.routeDeserializer;
        this.configDeserializer = builder.configDeserializer;
    }

    /**
     * Try to create root path if not exists.
     * <p>
     * This method creates root path if it is not exists. This can be needed because watcher can be added for
     * existent paths only.
     */
    public void tryCreateRootIfNotExists() {
        try {
            if (!curatorClient.exists(rootPath)) {
                curatorClient.create(rootPath, new byte[0]);
            }
        } catch (Exception e) {
            LOGGER.error("cannot create route path {}: {}", rootPath, e.getMessage(), e);
        }
    }

    /**
     * Fetch all existent routes file names.
     * <p>
     * Method returns list of relative paths relative to root path given in builder. These paths follow the template
     * {@code "<route id as UUID string>.json"}.
     *
     * @param watcher Root path children list watcher.
     * @return List routes file names (relative paths).
     */
    public List<String> fetchAllRoutesFilesNames(CuratorWatcher watcher) {
        try {
            if (watcher != null) {
                return curatorClient.children(rootPath, watcher);
            } else {
                return curatorClient.children(rootPath);
            }
        } catch (Exception e) {
            throw new IllegalStateException(String.format("cannot read routes list: %s", e.getMessage()), e);
        }
    }

    /**
     * Fetch engine config.
     * <p>
     * Reads config data from root path and deserialize it using given {@link EngineConfigDeserializer} implementation.
     *
     * @param watcher Engine content watcher.
     * @return Deserialized engine config stored in root path or {@code null}.
     */
    public EngineConfig fetchEngineConfig(CuratorWatcher watcher) {
        try {
            byte[] data = curatorClient.read(rootPath, watcher)
                    .getData()
                    .orElse(null);
            if (data == null || data.length == 0) {
                return null;
            }
            return configDeserializer.deserialize(data);
        } catch (Exception e) {
            throw new IllegalStateException(
                    String.format("cannot read engine config at '%s': %s", rootPath, e.getMessage()), e
            );
        }
    }

    /**
     * Fetches route using given id.
     *
     * @param id      Identifier of required route.
     * @param watcher Route content watcher.
     * @return Returns required route or {@code null} if it is not exists.
     */
    public Route fetchRouteById(UUID id, CuratorWatcher watcher) {
        return fetchRouteByAbsolutePath(PathUtil.createAbsolutePathFromRouteId(rootPath, id), watcher);
    }

    /**
     * Fetch route using given relative path.
     *
     * @param relativePath Relative path of required route.
     * @param watcher      Route content watcher.
     * @return Returns required route or {@code null} if it is not exists.
     * @see #fetchAllRoutesFilesNames
     */
    public Route fetchRouteByRelativePath(String relativePath, CuratorWatcher watcher) {
        return fetchRouteByAbsolutePath(rootPath + "/" + relativePath, watcher);
    }

    /**
     * Fetch route using given absolute path.
     *
     * @param absolutePath Absolute path of required route.
     * @param watcher      Route content watcher.
     * @return Returns required route or {@code null} if it is not exists.
     */
    public Route fetchRouteByAbsolutePath(String absolutePath, CuratorWatcher watcher) {
        UUID routeId = PathUtil.extractRouteIdFromAbsolutePath(rootPath, absolutePath);
        try {
            ReadResult result = curatorClient.read(absolutePath, watcher);
            if (result.status() == ReadResult.Status.NOT_FOUND) {
                return null;
            }
            return routeDeserializer.deserialize(routeId, result.getData().orElseThrow());
        } catch (Exception e) {
            throw new IllegalStateException(String.format("cannot read route %s data: %s", routeId, e.getMessage()), e);
        }
    }

    /**
     * Register connection state listener.
     *
     * @param connectionStateListener Connection state listener.
     * @param executor                Executor that will perform execution of listener.
     * @see CuratorClient#registerConnectionStateListener
     */
    public void registerConnectionStateListener(ConnectionStateListener connectionStateListener, Executor executor) {
        curatorClient.registerConnectionStateListener(connectionStateListener, executor);
    }

    /**
     * Remove connection state listener.
     *
     * @param connectionStateListener Connection state listener to remove.
     */
    public void removeConnectionStateListener(ConnectionStateListener connectionStateListener) {
        curatorClient.removeConnectionStateListener(connectionStateListener);
    }

    public static class Builder {
        private CuratorClient curatorClient;
        private String rootPath;
        private RouteDeserializer routeDeserializer;
        private EngineConfigDeserializer configDeserializer;

        private Builder() {
        }

        /**
         * ZNode path in ZooKeeper where configuration stored.
         * <p>
         * Required parameter.
         *
         * @param rootPath Root path.
         * @return Builder object.
         */
        public Builder withRootPath(String rootPath) {
            this.rootPath = rootPath;
            return this;
        }

        /**
         * Curator client object that will be used for communication with ZooKeeper.
         * <p>
         * Required parameter.
         *
         * @param curatorClient Curator client object.
         * @return Builder object.
         */
        public Builder withCuratorClient(CuratorClient curatorClient) {
            this.curatorClient = curatorClient;
            return this;
        }

        /**
         * Routes deserializer that will be used by task.
         *
         * @param routeDeserializer Route deserializer.
         * @return Builder object.
         */
        public Builder withRouteDeserializer(RouteDeserializer routeDeserializer) {
            this.routeDeserializer = routeDeserializer;
            return this;
        }

        /**
         * Engine config deserializer that will be used by task.
         * <p>
         * Required parameter.
         *
         * @param configDeserializer Engine config deserializer.
         * @return Builder object.
         */
        public Builder withConfigDeserializer(EngineConfigDeserializer configDeserializer) {
            this.configDeserializer = configDeserializer;
            return this;
        }

        /**
         * Build read repository.
         *
         * @return Created repository object.
         */
        public ZookeeperReadRepository build() {
            Objects.requireNonNull(rootPath, "rootPath");
            Objects.requireNonNull(curatorClient, "curatorClient");
            Objects.requireNonNull(routeDeserializer, "routeDeserializer");
            Objects.requireNonNull(configDeserializer, "configDeserializer");
            return new ZookeeperReadRepository(this);
        }
    }
}
