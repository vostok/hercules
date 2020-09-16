package ru.kontur.vostok.hercules.util.concurrent;

import org.junit.Assert;
import org.junit.Test;
import ru.kontur.vostok.hercules.util.time.TimeSource;
import ru.kontur.vostok.hercules.util.time.Timer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Gregory Koshelev
 */
public class LifoSemaphoreTest {
    @Test
    public void shouldAcquireAndReleasePermits() {
        LifoSemaphore semaphore = new LifoSemaphore(1_000);
        Assert.assertEquals(1_000, semaphore.availablePermits());

        Assert.assertTrue(semaphore.tryAcquire(1_000));
        Assert.assertEquals(0, semaphore.availablePermits());

        Assert.assertFalse(semaphore.tryAcquire(1));
        Assert.assertEquals(0, semaphore.availablePermits());

        semaphore.release(500);
        Assert.assertEquals(500, semaphore.availablePermits());

        Assert.assertTrue(semaphore.tryAcquire(1));
        Assert.assertEquals(499, semaphore.availablePermits());

        Assert.assertFalse(semaphore.tryAcquire(500));
        Assert.assertEquals(499, semaphore.availablePermits());

        semaphore.release(500);
        Assert.assertEquals(499 + 500, semaphore.availablePermits());
    }

    @Test
    public void shouldProcessNormallyIfNoSaturation() throws InterruptedException {
        int saturationConcurrency = 100;
        LifoSemaphore semaphore = new LifoSemaphore(saturationConcurrency);

        AtomicInteger success = new AtomicInteger(0);
        AtomicInteger failed = new AtomicInteger(0);

        List<Thread> threads = new ArrayList<>(saturationConcurrency);
        for (int i = 0; i < saturationConcurrency; i++) {
            Thread thread = new Thread(() -> {
                try {
                    if (semaphore.tryAcquire(1, 50, TimeUnit.MILLISECONDS)) {
                        success.incrementAndGet();
                        semaphore.release(1);
                    } else {
                        failed.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
            thread.start();
            threads.add(thread);
        }

        for (Thread thread : threads) {
            thread.join();
        }

        Assert.assertEquals(saturationConcurrency, success.get());
        Assert.assertEquals(0, failed.get());
        Assert.assertEquals(saturationConcurrency, semaphore.availablePermits());
    }

    @Test
    public void shouldWaitAndFailIfNoAvailablePermits() throws InterruptedException {
        LifoSemaphore semaphore = new LifoSemaphore(0);

        Timer timer = TimeSource.SYSTEM.timer(25);
        Assert.assertFalse(semaphore.tryAcquire(1, 50, TimeUnit.MILLISECONDS));
        Assert.assertTrue(timer.isExpired());
    }
}
