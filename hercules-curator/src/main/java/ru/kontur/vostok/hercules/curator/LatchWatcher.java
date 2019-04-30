package ru.kontur.vostok.hercules.curator;

import org.apache.curator.framework.api.CuratorWatcher;
import org.apache.zookeeper.WatchedEvent;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The reusable watcher can be applied only if it's latch is open.
 *
 * @author Gregory Koshelev
 */
public class LatchWatcher implements CuratorWatcher {
    private final AtomicBoolean latched = new AtomicBoolean(false);
    private final CuratorWatcher watcher;

    public LatchWatcher(CuratorWatcher watcher) {
        this.watcher = watcher;
    }

    /**
     * Latch the watcher. The watcher must be latched on applying.<br>
     * If {@link #latch()} is unsuccessful, thus the watcher should not be applied.
     *
     * @return true if it was successfully latched
     */
    public boolean latch() {
        return latched.compareAndSet(false, true);
    }

    /**
     * Reopen the watcher's latch and call underlying watcher.
     *
     * @param event the event
     * @throws Exception the exception is thrown by underlying watcher
     */
    @Override
    public void process(WatchedEvent event) throws Exception {
        latched.set(false);
        watcher.process(event);
    }
}
