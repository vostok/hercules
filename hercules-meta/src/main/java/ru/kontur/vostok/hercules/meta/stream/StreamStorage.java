package ru.kontur.vostok.hercules.meta.stream;

import org.apache.curator.framework.api.CuratorWatcher;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.apache.zookeeper.Watcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.curator.CuratorClient;
import ru.kontur.vostok.hercules.curator.LatchWatcher;
import ru.kontur.vostok.hercules.util.lifecycle.Lifecycle;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Stream storage is read-only caching proxy for {@link Stream} metadata.
 *
 * @author Innokentiy Krivonosov
 */
public class StreamStorage implements Lifecycle {
    private static final Logger LOGGER = LoggerFactory.getLogger(StreamStorage.class);

    private final AtomicReference<Map<String, Stream>> streams = new AtomicReference<>(new HashMap<>());

    private final CuratorClient curatorClient;
    private final StreamRepository repository;

    /**
     * The reusable watcher
     * <p>
     * Perform update by data change events
     */
    private final LatchWatcher latchWatcher = new LatchWatcher(event -> {
        if (event.getType() == Watcher.Event.EventType.None) {
            return;
        }
        update();
    });

    /**
     * Connection policy for ZooKeeper.
     * <p>
     * If connection is established after lost then should perform reinitialization because the events about
     * configuration changes may have been lost.
     */
    private final ConnectionStateListener connectionStateListener = (c, newState) -> {
        if (newState.isConnected()) {
            latchWatcher.release();
            update();
        }
    };

    public StreamStorage(CuratorClient curatorClient, StreamRepository repository) {
        this.curatorClient = curatorClient;
        this.repository = repository;
    }

    @Override
    public void start() {
        update();
        curatorClient.registerConnectionStateListener(connectionStateListener);
    }

    /**
     * Get Stream from local cache.
     *
     * @param name of the Stream
     * @return Optional of the found Stream or empty otherwise
     */
    public Optional<Stream> get(String name) {
        return Optional.ofNullable(streams.get().get(name));
    }

    private void update() {
        try {
            List<String> newNames = latchWatcher.latch() ? list(latchWatcher) : repository.list();
            Map<String, Stream> newStreams = getNewStreams(newNames);

            streams.set(newStreams);
        } catch (Exception e) {
            latchWatcher.release();
            LOGGER.error("Error on getting streams", e);
        }
    }

    private Map<String, Stream> getNewStreams(List<String> newNames) throws Exception {
        Map<String, Stream> newStreams = new HashMap<>(newNames.size());
        for (String name : newNames) {
            Optional<Stream> stream = repository.read(name);
            stream.ifPresent(value -> newStreams.put(name, value));
        }
        return newStreams;
    }

    private List<String> list(CuratorWatcher curatorWatcher) throws Exception {
        return curatorClient.children(StreamRepository.Z_PREFIX, curatorWatcher);
    }

    @Override
    public boolean stop(long timeout, TimeUnit timeUnit) {
        curatorClient.removeConnectionStateListener(connectionStateListener);
        return true;
    }
}
