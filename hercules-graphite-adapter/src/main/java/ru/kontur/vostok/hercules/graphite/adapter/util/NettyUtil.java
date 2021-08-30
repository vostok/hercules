package ru.kontur.vostok.hercules.graphite.adapter.util;

import io.netty.util.concurrent.Future;

/**
 * @author Gregory Koshelev
 */
public final class NettyUtil {
    public static void syncAll(Future<?> a, Future<?> b) throws InterruptedException {
        a.sync();
        b.sync();
    }

    public static void syncAll(Future<?>... futures) throws InterruptedException {
        for (Future<?> future : futures) {
            future.sync();
        }
    }

    private NettyUtil() {
        /* static class */
    }
}
