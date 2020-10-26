package ru.kontur.vostok.hercules.util.concurrent;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The topology that provides an inter-thread-isolated iteration.
 *
 * @author Gregory Koshelev
 */
public class ThreadLocalTopology<T> extends Topology<T> {
    public ThreadLocalTopology(T[] topology) {
        super(topology);
    }

    @Override
    protected State initiateState(Object[] array) {
        return new ThreadLocalState(array);
    }

    private static final class ThreadLocalState extends State {
        // Each thread has own "iterator".
        // An iteration starts with a random element for better balancing across the topology.
        private final ThreadLocal<AtomicInteger> it = ThreadLocal.withInitial(() -> new AtomicInteger(ThreadLocalRandom.current().nextInt()));

        protected ThreadLocalState(Object[] array) {
            super(array);
        }

        @Override
        protected int seed() {
            return it.get().getAndIncrement();
        }
    }
}
