package ru.kontur.vostok.hercules.util.fsm;

import java.util.HashMap;
import java.util.Map;

/**
 * TransitionMapBuilder
 *
 * @author Kirill Sulim
 */
public class TransitionMapBuilder<T extends State> {

    private final Map<T, T> map = new HashMap<>();

    private TransitionMapBuilder() {
    }

    public static <T extends State> TransitionMapBuilder<T> start() {
        return new TransitionMapBuilder<>();
    }

    public TransitionMapBuilder<T> transition(Transition<T> transition) {
        map.put(transition.getFrom(), transition.getTo());
        return this;
    }

    public Map<T, T> build() {
        return map;
    }
}
