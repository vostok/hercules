package ru.kontur.vostok.hercules.util.fsm;

/**
 * Transition
 *
 * @author Kirill Sulim
 */
public class Transition<T extends State> {

    private final T from;
    private final T to;

    private Transition(T from, T to) {
        this.from = from;
        this.to = to;
    }

    public T getFrom() {
        return from;
    }

    public T getTo() {
        return to;
    }

    public static <T extends State> Transition<T> of(T from, T to) {
        return new Transition<>(from, to);
    }
}
