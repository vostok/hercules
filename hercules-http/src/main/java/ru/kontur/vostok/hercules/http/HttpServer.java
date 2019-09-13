package ru.kontur.vostok.hercules.http;

import ru.kontur.vostok.hercules.http.handler.HttpHandler;
import ru.kontur.vostok.hercules.util.parameter.Parameter;
import ru.kontur.vostok.hercules.util.validation.IntegerValidators;
import ru.kontur.vostok.hercules.util.validation.LongValidators;
import ru.kontur.vostok.hercules.util.validation.ValidationResult;
import ru.kontur.vostok.hercules.util.validation.Validators;

import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Gregory Koshelev
 */
public abstract class HttpServer {
    private final AtomicReference<HttpServerState> state = new AtomicReference<>(HttpServerState.INIT);

    protected final String host;
    protected final int port;
    protected final Properties properties;
    protected final HttpHandler handler;

    public HttpServer(String host, int port, Properties properties, HttpHandler handler) {
        this.host = host;
        this.port = port;
        this.properties = properties;
        this.handler = handler;
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

    protected void startInternal() {
    }

    protected boolean stopInternal(long timeout, TimeUnit unit) {
        return true;
    }

    public static final class Props {
        public static final Parameter<Long> MAX_CONTENT_LENGTH =
                Parameter.longParameter("maxContentLength").
                        withDefault(HttpServerDefaults.DEFAULT_MAX_CONTENT_LENGTH).
                        withValidator(LongValidators.positive()).
                        build();

        public static final Parameter<Integer> CONNECTION_THRESHOLD =
                Parameter.integerParameter("connection.threshold").
                        withDefault(HttpServerDefaults.DEFAULT_CONNECTION_THRESHOLD).
                        withValidator(IntegerValidators.positive()).
                        build();

        public static final Parameter<Integer> IO_THREADS =
                Parameter.integerParameter("ioThreads").
                        withValidator(IntegerValidators.positive()).
                        required().
                        build();

        public static final Parameter<Integer> WORKER_THREADS =
                Parameter.integerParameter("workerThreads").
                        withValidator(IntegerValidators.positive()).
                        required().
                        build();

        public static final Parameter<String> ROOT_PATH =
                Parameter.stringParameter("rootPath").
                        withDefault("/").
                        withValidator(Validators.and(
                                Validators.notNull(),
                                x -> x.startsWith("/") ? ValidationResult.ok() : ValidationResult.error("Should start with '/'"))).
                        build();

        private Props() {
            /* static class */
        }
    }
}
