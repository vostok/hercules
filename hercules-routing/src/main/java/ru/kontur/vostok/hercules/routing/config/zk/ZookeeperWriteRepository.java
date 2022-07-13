package ru.kontur.vostok.hercules.routing.config.zk;

import java.util.Objects;
import java.util.UUID;

import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.curator.CuratorClient;
import ru.kontur.vostok.hercules.routing.engine.EngineConfig;
import ru.kontur.vostok.hercules.routing.engine.EngineConfigSerializer;
import ru.kontur.vostok.hercules.routing.engine.Route;
import ru.kontur.vostok.hercules.routing.engine.RouteSerializer;

/**
 * Repository for making changes into router config stored in ZooKeeper.
 *
 * @author Aleksandr Yuferov
 */
public class ZookeeperWriteRepository {
    private static final Logger LOGGER = LoggerFactory.getLogger(ZookeeperWriteRepository.class);
    private final CuratorClient curatorClient;
    private final String rootPath;
    private final RouteSerializer routeSerializer;
    private final EngineConfigSerializer configSerializer;

    public static Builder builder() {
        return new Builder();
    }

    protected ZookeeperWriteRepository(Builder builder) {
        this.curatorClient = builder.curatorClient;
        this.rootPath = builder.rootPath;
        this.routeSerializer = builder.routeSerializer;
        this.configSerializer = builder.configSerializer;
    }

    /**
     * Store engine config.
     *
     * @param engineConfig Engine config.
     * @return {@code true} if operation is successful.
     */
    public boolean tryStoreEngineConfig(EngineConfig engineConfig) {
        try {
            byte[] serialized = configSerializer.serialize(engineConfig);
            if (!curatorClient.exists(rootPath)) {
                return curatorClient.create(rootPath, serialized).isSuccess();
            } else {
                return curatorClient.update(rootPath, serialized).isSuccess();
            }
        } catch (Exception exception) {
            LOGGER.error("an error occurred while store engine config", exception);
            return false;
        }
    }

    /**
     * Store new route config.
     *
     * @param route Route to store (id must be null).
     * @return Identifier of created route or {@code null} if error occurred.
     */
    public UUID tryCreateRoute(Route route) {
        Preconditions.checkArgument(route.id() == null, "route id must be null");
        try {
            UUID id = UUID.randomUUID();
            byte[] serialized = routeSerializer.serialize(route);
            String absolutePath = PathUtil.createAbsolutePathFromRouteId(rootPath, id);
            if (curatorClient.create(absolutePath, serialized).isSuccess()) {
                return id;
            }
        } catch (Exception exception) {
            LOGGER.error("an error occurred while creating route", exception);
        }
        return null;
    }

    /**
     * Update already existent route.
     *
     * @param route New route state (id must not be null).
     * @return {@code true} if operation is successful.
     */
    public boolean tryUpdateRoute(Route route) {
        Preconditions.checkArgument(route.id() != null, "route id must not be null");
        try {
            byte[] serialized = routeSerializer.serialize(route);
            String absolutePath = PathUtil.createAbsolutePathFromRouteId(rootPath, route.id());
            return curatorClient.update(absolutePath, serialized).isSuccess();
        } catch (Exception exception) {
            LOGGER.error("an error occurred while updating route", exception);
            return false;
        }
    }

    /**
     * Remove route by id.
     *
     * @param routeId Route id.
     * @return {@code true} if operation is successful.
     */
    public boolean tryRemoveRouteById(UUID routeId) {
        Preconditions.checkArgument(routeId != null, "route id cannot be null");
        try {
            String absolutePath = PathUtil.createAbsolutePathFromRouteId(rootPath, routeId);
            return curatorClient.delete(absolutePath).isSuccess();
        } catch (Exception exception) {
            LOGGER.error("an error occurred while deleting route", exception);
            return false;
        }
    }

    public static class Builder {
        private CuratorClient curatorClient;
        private String rootPath;
        private RouteSerializer routeSerializer;
        private EngineConfigSerializer configSerializer;

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
         * Curator client object that will be used for communication with ZooKeeper by repository.
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
         * Route serializer that will be used by repository.
         * <p>
         * Required parameter.
         *
         * @param routeSerializer Route serializer.
         * @return Builder object.
         */
        public Builder withRouteSerializer(RouteSerializer routeSerializer) {
            this.routeSerializer = routeSerializer;
            return this;
        }

        /**
         * Engine config serializer that will be used by repository.
         * <p>
         * Required parameter.
         *
         * @param configSerializer Engine config serializer.
         * @return Builder object.
         */
        public Builder withConfigSerializer(EngineConfigSerializer configSerializer) {
            this.configSerializer = configSerializer;
            return this;
        }

        public ZookeeperWriteRepository build() {
            Objects.requireNonNull(rootPath, "rootPath");
            Objects.requireNonNull(curatorClient, "curatorClient");
            Objects.requireNonNull(routeSerializer, "routeSerializer");
            Objects.requireNonNull(configSerializer, "configSerializer");
            return new ZookeeperWriteRepository(this);
        }
    }
}
