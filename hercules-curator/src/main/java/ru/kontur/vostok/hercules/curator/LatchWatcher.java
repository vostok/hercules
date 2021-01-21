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
     * Release the watcher's latch.
     * <p>
     * The latch must be released if the watcher was applied but operation thrown an exception
     * Otherwise the applied watcher will no longer be called.
     *
     * @return {@code true} if it was successfully released
     */
    public boolean release() {
        return latched.compareAndSet(true, false);
    }

    /**
     * Release the watcher's latch and call underlying watcher.
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
