package ru.kontur.vostok.hercules.util;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.NavigableSet;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Pool of reusable ByteBuffers.
 * <p>
 * Class is thread-safe.
 *
 * @author Gregory Koshelev
 */
public class ByteBufferPool {
    private static final NavigableSet<ByteBufferWrapper> buffers = new ConcurrentSkipListSet<>();

    private static final AtomicLong totalCapacity = new AtomicLong(0);
    private static final long totalCapacitySoftLimit = VmUtil.maxDirectMemory() / 2;// 50%
    private static final long totalCapacityHardLimit = VmUtil.maxDirectMemory() * 3 / 4;// 75%

    /**
     * Acquire {@link ByteBuffer} with capacity is at least {@code size}.
     * <p>
     * If no appropriate buffer in the pool, then create new one.
     *
     * @param capacity a minimum buffer capacity
     * @return the byte buffer
     */
    public static ByteBuffer acquire(int capacity) {
        ByteBufferWrapper buffer = buffers.tailSet(ByteBufferWrapper.stub(capacity), true).pollFirst();
        if (buffer != null) {
            totalCapacity.addAndGet(-buffer.capacity);
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
     * Released {@link ByteBuffer} is ready for using as {@link ByteBuffer#clear()} has been called.
     *
     * @param buffer the byte buffer
     */
    public static void release(ByteBuffer buffer) {
        buffer.clear();
        int capacity = buffer.capacity();
        long currentTotalCapacity;
        do {
            currentTotalCapacity = totalCapacity();
            if (totalCapacity() + capacity > totalCapacityHardLimit) {
                return;
            }
        } while (!totalCapacity.compareAndSet(currentTotalCapacity, currentTotalCapacity + capacity));
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
            if (buffer.capacity >= capacity) {
                return buffer.buffer;
            }

            freedCapacity += buffer.capacity;
        } while (freedCapacity < capacity);
        return null;
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
