package ru.kontur.vostok.hercules.application;

/**
 * @author Gregory Koshelev
 */
public enum ApplicationState {
    INIT(0),
    STARTING(1),
    RUNNING(2),
    STOPPED(3);

    private final int order;

    ApplicationState(int order) {
        this.order = order;
    }

    public int order() {
        return order;
    }
}
