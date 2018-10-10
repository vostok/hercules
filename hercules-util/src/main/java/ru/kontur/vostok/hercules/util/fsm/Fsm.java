package ru.kontur.vostok.hercules.util.fsm;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Fsm - thread safe finite state machine implementation
 *
 * @author Kirill Sulim
 */
public class Fsm<T extends State> {

    private static class StateWithFuture<T extends State> {

        private final T state;
        private final CompletableFuture<StateWithFuture<T>> next;

        public StateWithFuture(T state, CompletableFuture<StateWithFuture<T>> next) {
            this.state = state;
            this.next = next;
        }

        public State getState() {
            return state;
        }

        public Future<StateWithFuture<T>> getNext() {
            return next;
        }
    }

    private final AtomicReference<StateWithFuture<T>> state;

    public Fsm(T state) {
        this.state = new AtomicReference<>(new StateWithFuture<>(state, new CompletableFuture<>()));
    }

    /**
     * Try to perform set of transitions
     *
     * @param transitionMap map of state and corresponding transition
     * @return empty optional if no transition can be performed else new FSM state
     */
    protected Optional<T> makeTransition(Map<T, T> transitionMap) {
        do {
            StateWithFuture<T> currentStateWithFuture = state.get();
            T newState = transitionMap.get(currentStateWithFuture.state);
            if (Objects.isNull(newState)) {
                return Optional.empty();
            }
            StateWithFuture<T> next = new StateWithFuture<>(newState, new CompletableFuture<>());
            if (state.compareAndSet(currentStateWithFuture, next)) {
                currentStateWithFuture.next.complete(next);
                return Optional.of(newState);
            }
        } while (true);
    }

    public T getState() {
        return state.get().state;
    }

    /**
     * Wait until next state change.
     *
     * @return new state
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public T waitStateChange() throws ExecutionException, InterruptedException {
        return state.get().next.get().state;
    }

    /**
     * Wait until next state will be one of {@code states}.
     *
     * @param states list of states to wait
     * @return new state
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public T waitForState(T ... states) throws ExecutionException, InterruptedException {
        final HashSet<T> statesSet = new HashSet<>(Arrays.asList(states));

        StateWithFuture<T> currentStateWithFuture = this.state.get();
        while (!statesSet.contains(currentStateWithFuture.state)) {
            currentStateWithFuture = currentStateWithFuture.next.get();
        }
        return currentStateWithFuture.state;
    }
}
