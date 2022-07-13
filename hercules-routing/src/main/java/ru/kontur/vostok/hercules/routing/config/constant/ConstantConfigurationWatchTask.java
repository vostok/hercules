package ru.kontur.vostok.hercules.routing.config.constant;

import ru.kontur.vostok.hercules.routing.config.ConfigurationObserver;
import ru.kontur.vostok.hercules.routing.config.ConfigurationWatchTask;
import ru.kontur.vostok.hercules.routing.engine.EngineConfig;
import ru.kontur.vostok.hercules.routing.engine.Route;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Constant configuration watch task.
 * <p>
 * This implementation can be used for rigid configuration or testing. When the {@link #start} method is invoked
 * then observer's {@link ConfigurationObserver#init} method will be invoked with engine config and routes list
 * given in constructor of the task.
 *
 * @author Aleksandr Yuferov
 */
public class ConstantConfigurationWatchTask implements ConfigurationWatchTask {
    private final EngineConfig engineConfig;
    private final List<? extends Route> routes;
    private ConfigurationObserver observer;

    /**
     * Constructor.
     *
     * @param engineConfig Router engine config that will be passed to observer.
     * @param routes       Routes list that will be passed to observer.
     */
    public ConstantConfigurationWatchTask(EngineConfig engineConfig, List<? extends Route> routes) {
        this.engineConfig = engineConfig;
        this.routes = routes;
    }

    /**
     * Invokes observer's {@link ConfigurationObserver#init} with parameters given in constructor of this object.
     *
     * @param observer Configuration observer object
     */
    @Override
    public void start(ConfigurationObserver observer) {
        this.observer = observer;
        this.observer.init(engineConfig, routes);
    }

    @Override
    public boolean stop(long timeout, TimeUnit timeUnit) {
        observer = null;
        return true;
    }

    public void reinitialize(EngineConfig engineConfig, List<? extends Route> routes) {
        observer.init(engineConfig, routes);
    }

    public void addRoute(Route route) {
        observer.onRouteCreated(route);
    }

    public void changeRoute(Route route) {
        observer.onRouteChanged(route);
    }

    public void removeRoute(UUID routeId) {
        observer.onRouteRemoved(routeId);
    }

    public void changeEngineConfig(EngineConfig config) {
        observer.onEngineConfigChanged(config);
    }
}
