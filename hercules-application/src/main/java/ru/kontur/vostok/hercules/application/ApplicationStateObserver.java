package ru.kontur.vostok.hercules.application;

/**
 * Interface of the classes that will observe the {@link Application application} state.
 *
 * @author Aleksandr Yuferov
 */
public interface ApplicationStateObserver {

    /**
     * Application state changed handler.
     *
     * @param app        {@link Application} instance.
     * @param newState   New state of application instance.
     * @throws Exception Will be caught by {@link Application} class, log it and suppress.
     */
    void onApplicationStateChanged(Application app, ApplicationState newState) throws Exception;
}
