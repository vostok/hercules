package ru.kontur.vostok.hercules.application;

/**
 * @author Aleksandr Yuferov
 */
public class NoopApplicationStateObserver implements ApplicationStateObserver {

    @Override
    public void onApplicationStateChanged(Application app, ApplicationState newState) {
    }
}
