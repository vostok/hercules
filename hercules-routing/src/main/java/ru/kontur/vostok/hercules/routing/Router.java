package ru.kontur.vostok.hercules.routing;

import ru.kontur.vostok.hercules.routing.config.ConfigurationWatchTask;
import ru.kontur.vostok.hercules.routing.engine.RouterEngine;
import ru.kontur.vostok.hercules.util.lifecycle.Lifecycle;

import java.util.concurrent.TimeUnit;

/**
 * @author Aleksandr Yuferov
 */
public class Router<Q, D extends Destination<D>> implements Lifecycle {
    private final ConfigurationWatchTask configWatchTask;
    private final RouterEngine<Q> engine;

    public Router(ConfigurationWatchTask configWatchTask, RouterEngine<Q> engine) {
        this.configWatchTask = configWatchTask;
        this.engine = engine;
    }

    public D route(Q query) {
        @SuppressWarnings("unchecked")
        D destination = (D) engine.route(query);
        return destination;
    }

    @Override
    public void start() {
        configWatchTask.start(engine);
    }

    @Override
    public boolean stop(long timeout, TimeUnit timeUnit) {
        return configWatchTask.stop(timeout, timeUnit);
    }
}
