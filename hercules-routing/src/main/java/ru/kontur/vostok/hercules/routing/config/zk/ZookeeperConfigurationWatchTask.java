package ru.kontur.vostok.hercules.routing.config.zk;

import com.google.common.base.Preconditions;
import org.apache.curator.framework.api.CuratorWatcher;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import ru.kontur.vostok.hercules.routing.config.ConfigurationObserver;
import ru.kontur.vostok.hercules.routing.config.ConfigurationWatchTask;
import ru.kontur.vostok.hercules.routing.engine.EngineConfig;
import ru.kontur.vostok.hercules.routing.engine.Route;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Configuration watch task implemented to watch configuration in ZooKeeper.
 * <p>
 * Creates an own single thread executor that handles events from ZooKeeper watchers and delegate handling changes to
 * {@link ConfigurationObserver}.
 *
 * @author Aleksandr Yuferov
 */
public class ZookeeperConfigurationWatchTask implements ConfigurationWatchTask {
    private static final int TASKS_QUEUE_CAPACITY = 100;
    private final CuratorWatcher concreteRouteWatcher = new DeferredWatcher(this::onConcreteRouteChanged);
    private final CuratorWatcher routeListWatcher = new DeferredWatcher(this::onRoutesListChanged);
    private final CuratorWatcher engineConfigWatcher = new DeferredWatcher(this::onEngineConfigChanged);

    /**
     * Connection policy for ZooKeeper.
     * <p>
     * If connection is established after lost then should perform full reinitialization because the events about
     * configuration changes may have been lost.
     */
    private final ConnectionStateListener connectionStateListener = (c, newState) -> {
        if (newState.isConnected()) {
            reinitialize();
        }
    };

    /**
     * Rejection execution policy.
     * <p>
     * If queue is full then easier to complete reinitialization. This policy clear the task queue and pass to executor
     * reinitialization task.
     */
    private final RejectedExecutionHandler rejectedExecutionHandler = (r, executor) -> {
        if (!executor.isShutdown()) {
            executor.getQueue().clear();
            executor.execute(this::reinitialize);
        }
    };

    private final AtomicBoolean started = new AtomicBoolean(false);
    private final Set<String> routesFilesNames = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final ZookeeperReadRepository repository;
    private ThreadPoolExecutor executorService;
    private ConfigurationObserver observer;

    /**
     * Constructor.
     *
     * @param readRepository ZooKeeper read repository.
     */
    public ZookeeperConfigurationWatchTask(ZookeeperReadRepository readRepository) {
        this.repository = readRepository;
    }

    /**
     * Start the task.
     * <p>
     * Starts the inner executor service and synchronously initialize data.
     *
     * @param newObserver Observer that will be notified about configuration events.
     */
    @Override
    public void start(ConfigurationObserver newObserver) {
        Preconditions.checkArgument(newObserver != null, "observer argument cannot be null");
        if (started.compareAndSet(false, true)) {
            observer = newObserver;
            executorService = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
                    new ArrayBlockingQueue<>(TASKS_QUEUE_CAPACITY), rejectedExecutionHandler);
            reinitialize();
            repository.registerConnectionStateListener(connectionStateListener, executorService);
        }
    }

    /**
     * Stop the task.
     * <p>
     * Shutdown inner executor service.
     *
     * @param timeout  Timeout
     * @param timeUnit Units of timeout
     * @return Returns true if shutdown successful.
     */
    @Override
    public boolean stop(long timeout, TimeUnit timeUnit) {
        if (started.compareAndSet(true, false)) {
            repository.removeConnectionStateListener(connectionStateListener);
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(timeout, TimeUnit.MILLISECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
            }
            executorService = null;
            observer = null;
            routesFilesNames.clear();
            return true;
        }
        return false;
    }

    /**
     * Full reinitialization.
     * <p>
     * Method reads engine configuration, reads all routes and subscribe to their changes using
     * {@link CuratorWatcher}s. Also, method clears the tasks queue to prevent unnecessary work.
     * After reading will invoke observer's {@link ConfigurationObserver#init} method and pass fetched data to it.
     */
    private void reinitialize() {
        executorService.getQueue().clear();
        repository.tryCreateRootIfNotExists();
        routesFilesNames.clear();
        List<String> routesRelativePaths = repository.fetchAllRoutesFilesNames(routeListWatcher);
        List<Route> routes = routesRelativePaths.stream()
                .map(relativePath -> repository.fetchRouteByRelativePath(relativePath, concreteRouteWatcher))
                .collect(Collectors.toList());
        routesFilesNames.addAll(routesRelativePaths);
        EngineConfig engineConfig = repository.fetchEngineConfig(engineConfigWatcher);
        observer.init(engineConfig, routes);
    }

    /**
     * Engine config events handler.
     * <p>
     * If event informs about engine config deletion then this method will trigger full reinitialization. If event
     * informs about root node data change then new engine config will be fetched and pass to observer's
     * {@link ConfigurationObserver#onEngineConfigChanged} method.
     *
     * @param event Event of root path from ZooKeeper.
     */
    private void onEngineConfigChanged(WatchedEvent event) {
        switch (event.getType()) {
            case NodeDeleted:
                reinitialize();
                break;
            case NodeDataChanged:
                observer.onEngineConfigChanged(repository.fetchEngineConfig(engineConfigWatcher));
                break;
        }
    }

    /**
     * Routes list event handler.
     * <p>
     * Method detects which routes have been created, fetch their data and pass it to observer's
     * {@link ConfigurationObserver#onRouteCreated} method and subscribes to their changes.
     * Also, method detects which routes have been deleted and reports about them to observer using
     * {@link ConfigurationObserver#onRouteRemoved} method.
     *
     * @param event Event of route list changes from ZooKeeper.
     */
    private void onRoutesListChanged(WatchedEvent event) {
        if (event.getType() != Watcher.Event.EventType.NodeChildrenChanged) {
            return;
        }
        List<String> newRoutesList = repository.fetchAllRoutesFilesNames(routeListWatcher);
        List<String> created = new ArrayList<>(newRoutesList.size() / 2);
        for (String relativePath : newRoutesList) {
            if (!routesFilesNames.contains(relativePath)) {
                Route route = repository.fetchRouteByRelativePath(relativePath, concreteRouteWatcher);
                if (route != null) {
                    created.add(relativePath);
                    observer.onRouteCreated(route);
                }
            }
        }
        for (String relativePath : routesFilesNames) {
            if (!newRoutesList.contains(relativePath)) {
                routesFilesNames.remove(relativePath);
                observer.onRouteRemoved(PathUtil.extractRouteIdFromRelativePath(relativePath));
            }
        }
        routesFilesNames.addAll(created);
    }

    /**
     * Concrete route event handler.
     * <p>
     * Reloads each route that has been changed and notifies observer via {@link ConfigurationObserver#onRouteChanged}.
     *
     * @param event Event of route data changes from ZooKeeper.
     */
    private void onConcreteRouteChanged(WatchedEvent event) {
        if (event.getType() != Watcher.Event.EventType.NodeDataChanged) {
            return;
        }
        Route route = repository.fetchRouteByAbsolutePath(event.getPath(), concreteRouteWatcher);
        if (route != null) {
            observer.onRouteChanged(route);
        }
    }

    private class DeferredWatcher implements CuratorWatcher {
        private final Consumer<WatchedEvent> consumer;

        private DeferredWatcher(Consumer<WatchedEvent> consumer) {
            this.consumer = consumer;
        }

        @Override
        public void process(WatchedEvent event) {
            if (event.getType() != Watcher.Event.EventType.None) {
                executorService.execute(() -> consumer.accept(event));
            }
        }
    }
}
