package ru.kontur.vostok.hercules.util;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.NavigableSet;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Pool of reusable ByteBuffers.
 * <p>
 * Pool serves for direct buffers only. Class is thread-safe.
 *
 * @author Gregory Koshelev
 */
public final class ByteBufferPool {
    private static final NavigableSet<ByteBufferWrapper> buffers = new ConcurrentSkipListSet<>();

    private static final AtomicInteger count = new AtomicInteger(0);
    private static final AtomicLong totalCapacity = new AtomicLong(0);
    private static final long totalCapacitySoftLimit = VmUtil.maxDirectMemory() / 2;// 50%
    private static final long totalCapacityHardLimit = VmUtil.maxDirectMemory() * 3 / 4;// 75%

    /**
     * Acquire {@link ByteBuffer} with capacity is at least {@code capacity} and limit is exactly equal to {@code capacity}.
     * <p>
     * If no appropriate buffer in the pool, then create new one.
     * <p>
     * Acquired buffer is ready for using. So, there is no reason to call {@link ByteBuffer#clear()} on it.
     * <p>
     * Also, calling {@link ByteBuffer#clear()} may be dangerous due to limit to be changed.
     *
     * @param capacity a minimum buffer capacity
     * @return the byte buffer
     */
    public static ByteBuffer acquire(int capacity) {
        ByteBufferWrapper buffer = buffers.tailSet(ByteBufferWrapper.stub(capacity), true).pollFirst();
        if (buffer != null) {
            totalCapacity.addAndGet(-buffer.capacity);
            count.decrementAndGet();
            buffer.buffer.limit(capacity);
            return buffer.buffer;
        } else {
            ByteBuffer bb;
            if (totalCapacity() < totalCapacitySoftLimit || (bb = tryAcquireOrFree(capacity)) == null) {
                return allocateNew(capacity);
            }
            return bb;
        }
    }

    /**
     * Release {@link ByteBuffer} into the pool.
     * <p>
     * Method accepts only reusable direct buffers, otherwise it will be no-op.
     * Also, if the pool exceeds memory limit then the buffer will be throw away.
     *
     * @param buffer the byte buffer
     */
    public static void release(ByteBuffer buffer) {
        if (!buffer.isDirect() || buffer.isReadOnly()) {
            return;
        }

        buffer.clear();
        int capacity = buffer.capacity();
        long currentTotalCapacity;
        do {
            currentTotalCapacity = totalCapacity();
            if (totalCapacity() + capacity > totalCapacityHardLimit) {
                return;
            }
        } while (!totalCapacity.compareAndSet(currentTotalCapacity, currentTotalCapacity + capacity));
        count.incrementAndGet();
        buffers.add(ByteBufferWrapper.wrap(buffer));
    }

    /**
     * Return total capacity of buffers inside the pool.
     *
     * @return total capacity
     */
    public static long totalCapacity() {
        return totalCapacity.get();
    }

    /**
     * Return count of buffers inside the pool.
     *
     * @return count
     */
    public static int count() {
        return count.get();
    }

    private static ByteBuffer allocateNew(int capacity) {
        try {
            return ByteBuffer.allocateDirect(capacity);
        } catch (OutOfMemoryError oom) {
            ByteBuffer buffer = tryAcquireOrFree(capacity);
            if (buffer != null) {
                return buffer;
            }
            throw oom;
        }
    }

    private static ByteBuffer tryAcquireOrFree(int capacity) {
        long freedCapacity = 0;
        do {
            ByteBufferWrapper buffer = buffers.pollLast();
            if (buffer == null) {
                return null;
            }
            totalCapacity.addAndGet(-buffer.capacity);
            count.decrementAndGet();
            if (buffer.capacity >= capacity) {
                buffer.buffer.limit(capacity);
                return buffer.buffer;
            }

            freedCapacity += buffer.capacity;
        } while (freedCapacity < capacity);
        return null;
    }

    private ByteBufferPool() {
        /* static class */
    }

    /**
     * Class provides comparable wrapper over byte buffer with identity equals.
     * Class is used to put byte buffers into {@link NavigableSet}.
     */
    private static class ByteBufferWrapper implements Comparable<ByteBufferWrapper> {
        private final ByteBuffer buffer;
        private final int capacity;

        private ByteBufferWrapper(ByteBuffer buffer, int capacity) {
            this.buffer = buffer;
            this.capacity = capacity;
        }

        /**
         * Stub wrapper without byte buffer inside.
         * <p>
         * Use only for searching in {@link ByteBufferPool#buffers} set.
         *
         * @param capacity capacity
         * @return stub wrapper
         */
        static ByteBufferWrapper stub(int capacity) {
            return new ByteBufferWrapper(null, capacity);
        }

        /**
         * Wrap byte buffer.
         *
         * @param buffer the byte buffer
         * @return wrapper
         */
        static ByteBufferWrapper wrap(ByteBuffer buffer) {
            return new ByteBufferWrapper(buffer, buffer.capacity());
        }

        @Override
        public int compareTo(@NotNull ByteBufferPool.ByteBufferWrapper o) {
            return capacity - o.capacity;
        }
    }
}
