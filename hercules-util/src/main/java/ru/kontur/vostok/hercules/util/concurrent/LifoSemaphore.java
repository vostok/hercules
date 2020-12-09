package ru.kontur.vostok.hercules.util.concurrent;

import ru.kontur.vostok.hercules.util.time.TimeSource;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.concurrent.locks.LockSupport;

/**
 * Implementation of semaphore where threads are processed in LIFO fashion.
 * <p>
 * See java doc of {@link java.util.concurrent.Semaphore} for more details.
 * <p>
 * Class is thread-safe.
 *
 * @author Gregory Koshelev
 */
public class LifoSemaphore {
    private final Node TAIL = new Node(null, null);
    private final Node HEAD = new Node(null, TAIL);

    private final AtomicLong permits;

    private final TimeSource time;

    public LifoSemaphore(long permits) {
        this(permits, TimeSource.SYSTEM);
    }

    LifoSemaphore(long permits, TimeSource time) {
        this.permits = new AtomicLong(permits);
        this.time = time;
    }

    /**
     * Try to acquire permits without waiting.
     *
     * @param amount the amount of permits
     * @return {@code true} if permits are acquired or {@code false} otherwise
     */
    public boolean tryAcquire(long amount) {
        checkPermits(amount);

        for (; ; ) {
            long availablePermits = availablePermits();
            long remainingPermits = availablePermits - amount;
            if (remainingPermits < 0) {
                return false;
            }
            if (permits.compareAndSet(availablePermits, remainingPermits)) {
                return true;
            }
        }
    }

    /**
     * Try to acquire permits for the specified timeout.
     *
     * @param amount  the amount of permits
     * @param timeout the maximum time to wait
     * @param unit    time unit
     * @return {@code true} if permits are acquired or {@code false} otherwise
     * @throws InterruptedException if thread was interrupted
     */
    public boolean tryAcquire(long amount, long timeout, TimeUnit unit) throws InterruptedException {
        checkPermits(amount);

        // Fast path: acquire permits without waiting
        if (tryAcquire(amount)) {
            return true;
        }

        Node node = addNode();

        long remainingNanos = unit.toNanos(timeout);
        long deadline = time.nanoseconds() + remainingNanos;

        // Try to acquire permits while there's still time
        for (; ; ) {
            boolean acquired = tryAcquire(amount);

            if (!acquired && (remainingNanos = deadline - time.nanoseconds()) > 0) {
                // Park thread as permits haven't been acquired and elapsed time doesn't exceed timeout
                LockSupport.parkNanos(remainingNanos);
                if (Thread.interrupted()) {
                    throw new InterruptedException();
                }
            } else {
                cleanup(node);

                node.active = false;

                wakeup();

                return acquired;
            }
        }
    }

    /**
     * Release permits.
     *
     * @param amount the amount of permits
     */
    public void release(long amount) {
        checkPermits(amount);

        for (; ; ) {
            long availablePermits = availablePermits();
            if (permits.compareAndSet(availablePermits, availablePermits + amount)) {
                wakeup();
                return;
            }
        }
    }

    /**
     * Return available permits.
     *
     * @return available permits
     */
    public long availablePermits() {
        return permits.get();
    }

    /**
     * Add node associated with the current thread to stack.
     *
     * @return node
     */
    private Node addNode() {
        Node node = new Node(Thread.currentThread(), null);
        for (; ; ) {
            Node current = HEAD.next;
            node.next = current;
            if (HEAD.casNext(current, node)) {
                break;
            }
        }
        return node;
    }

    /**
     * Wake up top waiting thread.
     * Thread associated with top node in stack.
     */
    private void wakeup() {
        do {
            Node top = HEAD.next;
            if (top != TAIL && top.active) {
                LockSupport.unpark(top.thread);
            }
        } while (cleanup(HEAD));
    }

    /**
     * Remove chain of inactive (i.e. processed) nodes starting from next node.
     *
     * @param node
     * @return {@code true} if at least one node has been removed
     */
    private boolean cleanup(Node node) {
        Node current = node.next;
        Node next = current;

        while (next != TAIL && !next.active) {
            next = next.next;
        }

        return current != next && node.casNext(current, next);
    }

    /**
     * Only the non-negative amount of permits is possible to release or acquire.
     *
     * @param amount the amount of permits
     * @throws IllegalArgumentException if amount is negative
     */
    private static void checkPermits(long amount) {
        if (amount < 0) {
            throw new IllegalArgumentException();
        }
    }

    private static class Node {
        private final Thread thread;
        private volatile Node next;
        private volatile boolean active = true;

        Node(Thread thread, Node next) {
            this.thread = thread;
            this.next = next;
        }

        /**
         * Atomically CAS the next node.
         *
         * @param current the current next node
         * @param update  the new one
         * @return {@code true} if next node is swapped or {@code false} otherwise
         */
        boolean casNext(Node current, Node update) {
            return nextFieldUpdater.compareAndSet(this, current, update);
        }

        private static final AtomicReferenceFieldUpdater<Node, Node> nextFieldUpdater =
                AtomicReferenceFieldUpdater.newUpdater(Node.class, Node.class, "next");
    }
}
