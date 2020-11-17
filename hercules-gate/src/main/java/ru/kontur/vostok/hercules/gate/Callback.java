package ru.kontur.vostok.hercules.gate;

/**
 * @author Gregory Koshelev
 */
public interface Callback {
    Callback EMPTY = () -> {
    };

    void call();

    static Callback empty() {
        return EMPTY;
    }
}
