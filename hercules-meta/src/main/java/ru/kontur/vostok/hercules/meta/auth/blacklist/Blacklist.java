package ru.kontur.vostok.hercules.meta.auth.blacklist;

import org.apache.zookeeper.Watcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.curator.CuratorClient;
import ru.kontur.vostok.hercules.curator.LatchWatcher;
import ru.kontur.vostok.hercules.util.concurrent.RenewableTask;
import ru.kontur.vostok.hercules.util.concurrent.RenewableTaskScheduler;
import ru.kontur.vostok.hercules.util.lifecycle.Lifecycle;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Gregory Koshelev
 */
public class Blacklist implements Lifecycle {

    private static final Logger LOGGER = LoggerFactory.getLogger(Blacklist.class);

    private static final String PATH = "/hercules/auth/blacklist";

    private final CuratorClient curatorClient;
    private final AtomicReference<ConcurrentHashMap<String, Object>> apiKeys = new AtomicReference<>(new ConcurrentHashMap<>());
    private final AtomicReference<State> state = new AtomicReference<>(State.INIT);
    private final RenewableTask updateTask;
    private final LatchWatcher latchWatcher;

    public Blacklist(CuratorClient curatorClient, RenewableTaskScheduler scheduler) {
        this.curatorClient = curatorClient;
        this.updateTask = scheduler.task(this::update, 60_000, false);
        this.latchWatcher = new LatchWatcher(event -> {
            if (event.getType() == Watcher.Event.EventType.None) {
                // We are only interested in the data changes
                //TODO: Process ZK reconnection separately by using CuratorFramework.getConnectionStateListenable()
                return;
            }
            updateTask.renew();
        });
    }

    public boolean contains(String apiKey) {
        if (state.get() != State.RUNNING) {
            throw new IllegalStateException("Invalid state of blacklist");
        }
        return apiKeys.get().containsKey(apiKey);
    }

    @Override
    public void start() {
        if (!state.compareAndSet(State.INIT, State.STARTING)) {
            throw new IllegalStateException("Invalid state of blacklist");
        }

        updateTask.renew();

        state.set(State.RUNNING);
    }

    @Override
    public boolean stop(long timeout, TimeUnit unit) {
        state.set(State.STOPPED);
        updateTask.disable();
        return true;
    }

    private void update() {
        if (state.get() == State.STOPPED) {
            return;
        }

        List<String> children;
        try {
            children = latchWatcher.latch() ? curatorClient.children(PATH, latchWatcher) : curatorClient.children(PATH);
        } catch (Exception e) {
            LOGGER.error("Error on updating list", e);
            return;
        }

        ConcurrentHashMap<String, Object> newApiKeys = new ConcurrentHashMap<>(children.size());
        for (String apiKey : children) {
            newApiKeys.put(apiKey, STUB);
        }
        apiKeys.set(newApiKeys);
    }

    private enum State {
        INIT,
        STARTING,
        RUNNING,
        STOPPED;
    }

    private static final Object STUB = new Object();
}
