package ru.kontur.vostok.hercules.application;

/**
 * @author Gregory Koshelev
 */
public enum ApplicationState {
    INIT(0),
    STARTING(10),
    RUNNING(20),
    STOPPING(30),
    STOPPED(40);

    private final int order;

    ApplicationState(int order) {
        this.order = order;
    }

    public int order() {
        return order;
    }
}
