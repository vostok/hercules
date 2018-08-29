package ru.kontur.vostok.hercules.http;

import ru.kontur.vostok.hercules.http.handler.AsyncHttpHandler;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Gregory Koshelev
 */
public abstract class AsyncHttpServer {
    private final AtomicReference<HttpServerState> state = new AtomicReference<>(HttpServerState.INIT);

    protected final String hostname;
    protected final int port;

    public AsyncHttpServer(String hostname, int port) {
        this.hostname = hostname;
        this.port = port;
    }

    public final void start() {
        if (!state.compareAndSet(HttpServerState.INIT, HttpServerState.STARTING)) {
            throw new IllegalStateException("Expect INIT state of Http server");
        }

        startInternal();

        if (!state.compareAndSet(HttpServerState.STARTING, HttpServerState.RUNNING)) {
            throw new IllegalStateException("Expect STARTING state of Http server");
        }
    }

    public final boolean stop(long timeout, TimeUnit unit) {
        if (!state.compareAndSet(HttpServerState.RUNNING, HttpServerState.STOPPING)) {
            throw new IllegalStateException("Expect RUNNING state of Http server");
        }

        boolean result = stopInternal(timeout, unit);

        if (!state.compareAndSet(HttpServerState.STOPPING, HttpServerState.STOPPED)) {
            throw new IllegalStateException("Expect STOPPING state of Http server");
        }

        return result;
    }

    public final void setHandler(AsyncHttpHandler handler) {
        if (!HttpServerState.INIT.equals(state.get())) {
            throw new IllegalStateException("Expect INIT state of Http server");
        }

        setHandlerInternal(handler);
    }

    protected void startInternal() {
    }

    protected boolean stopInternal(long timeout, TimeUnit unit) {
        return true;
    }

    protected abstract void setHandlerInternal(AsyncHttpHandler handler);
}
