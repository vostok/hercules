package ru.kontur.vostok.hercules.meta.blacklist;

import ru.kontur.vostok.hercules.meta.curator.CuratorClient;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Gregory Koshelev
 */
public class Blacklist {
    private final CuratorClient curatorClient;
    private final AtomicReference<ConcurrentHashMap<String, Object>> apiKeys = new AtomicReference<>(new ConcurrentHashMap<>());
    private final AtomicReference<State> state = new AtomicReference<>(State.INIT);

    public Blacklist(CuratorClient curatorClient) {
        this.curatorClient = curatorClient;
    }

    public boolean contains(String apiKey) {
        if (state.get() != State.RUNNING) {
            throw new IllegalStateException("Invalid state of blacklist");
        }
        return apiKeys.get().containsKey(apiKey);
    }

    public void start() throws Exception {
        if (!state.compareAndSet(State.INIT, State.STARTING)) {
            throw new IllegalStateException("Invalid state of blacklist");
        }

        update();

        state.set(State.RUNNING);
    }

    public void stop() {
        state.set(State.STOPPED);
    }

    private void update() throws Exception {
        if (state.get() == State.STOPPED) {
            return;
        }

        List<String> children = curatorClient.children("/hercules/auth/blacklist", e -> update());

        ConcurrentHashMap<String, Object> oldApiKeys = apiKeys.get();
        ConcurrentHashMap<String, Object> newApiKeys = new ConcurrentHashMap<>(children.size());
        for (String apiKey : children) {
            newApiKeys.put(apiKey, STUB);
        }
        apiKeys.compareAndSet(oldApiKeys, newApiKeys);
    }

    private enum State {
        INIT,
        STARTING,
        RUNNING,
        STOPPED;
    }

    private static final Object STUB = new Object();
}
