package ru.kontur.vostok.hercules.routing.config;

import ru.kontur.vostok.hercules.routing.engine.EngineConfig;
import ru.kontur.vostok.hercules.routing.engine.Route;

import java.util.List;
import java.util.UUID;

/**
 * Interface of routing configuration observer.
 *
 * @author Aleksandr Yuferov
 */
public interface ConfigurationObserver {
    /**
     * Initialize.
     * <p>
     * Can be invoked more than one time, i.e. after reconnection to a storage.
     *
     * @param engineConfig Router engine configuration from storage or {@code null} if it is not exists.
     * @param routes       Routes list from storage.
     */
    void init(EngineConfig engineConfig, List<? extends Route> routes);

    /**
     * New route has been created.
     *
     * @param route Route.
     */
    void onRouteCreated(Route route);

    /**
     * Route has been changed.
     *
     * @param route New route data (id does not change).
     */
    void onRouteChanged(Route route);

    /**
     * Route has been removed.
     *
     * @param routeId Removed route id.
     */
    void onRouteRemoved(UUID routeId);

    /**
     * Router engine config has been changed.
     *
     * @param engineConfig New router engine config state.
     */
    void onEngineConfigChanged(EngineConfig engineConfig);
}
