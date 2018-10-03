package ru.kontur.vostok.hercules.util.fsm;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Fsm - thread safe finite state machine implementation
 *
 * @author Kirill Sulim
 */
public class Fsm<T extends State> {

    private final AtomicReference<T> state;
    private volatile CountDownLatch latch = new CountDownLatch(1);

    public Fsm(T state) {
        this.state = new AtomicReference<>(state);
    }

    /**
     * Try to perform set of transitions
     *
     * @param transitionMap map of state and corresponding transition
     * @return true if on of transition is performed else false
     */
    public T makeTransition(Map<T, T> transitionMap) {
        do {
            T currentState = state.get();
            T newState = transitionMap.get(currentState);
            if (Objects.isNull(newState)) {
                return null;
            }
            if (state.compareAndSet(currentState, newState)) {
                latch.countDown();
                latch = new CountDownLatch(1);
                return newState;
            }
        } while (true);
    }

    public T getState() {
        return state.get();
    }

    /**
     * Wait until next state change.
     *
     * Warning: this method is not 100% thread safe, because state can be changed between latch.await() termination
     * and state.get();
     *
     * @return new state
     * @throws InterruptedException
     */
    public T waitStateChange() throws InterruptedException {
        latch.await();
        return state.get();
    }

    /**
     * Wait until next state will be one of {@code states}.
     *
     * Warning: this method is not 100% thread safe, because state can be changed between state.get() and waitStateChange()
     *
     * @param states list of states to wait
     * @return new state
     * @throws InterruptedException
     */
    public T waitForState(T ... states) throws InterruptedException {
        HashSet<T> setStates = new HashSet<>(Arrays.asList(states));
        T current = state.get();
        while (!setStates.contains(current)) {
            current = waitStateChange();
        }
        return current;
    }
}
