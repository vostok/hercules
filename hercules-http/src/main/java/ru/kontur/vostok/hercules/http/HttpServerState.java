package ru.kontur.vostok.hercules.http;

/**
 * @author Gregory Koshelev
 */
public enum HttpServerState {
    INIT,
    STARTING,
    RUNNING,
    STOPPING,
    STOPPED;
}
