package ru.kontur.vostok.hercules.application;

/**
 * States of the {@link Application} class.
 *
 * @author Gregory Koshelev
 */
public enum ApplicationState {
    /**
     * After construction state.
     */
    INIT,

    /**
     * Performing context construction.
     */
    STARTING,

    /**
     * Application in working condition.
     */
    RUNNING,

    /**
     * Starting of shutdown.
     */
    STOPPING,

    /**
     * Application doesn't work.
     */
    STOPPED,
}
