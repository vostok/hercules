package ru.kontur.vostok.hercules.undertow.util;

import io.undertow.Undertow;
import io.undertow.UndertowOptions;
import io.undertow.server.HttpServerExchange;
import org.xnio.Options;
import ru.kontur.vostok.hercules.http.HttpServer;
import ru.kontur.vostok.hercules.http.HttpServerRequest;
import ru.kontur.vostok.hercules.http.handler.ExceptionHandler;
import ru.kontur.vostok.hercules.http.handler.HttpHandler;
import ru.kontur.vostok.hercules.util.parameter.Parameter;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * Undertow library HTTP-server implementation.
 *
 * @author Gregory Koshelev
 */
public class UndertowHttpServer extends HttpServer {

    private Undertow undertow;

    /**
     * Constructor.
     *
     * @param host       HTTP-host.
     * @param port       TCP-port of HTTP-server.
     * @param properties Other properties.
     * @param handler    Root HTTP-handler.
     * @deprecated Use {@link #UndertowHttpServer(Properties, HttpHandler)} and pass host and port parameters using properties.
     */
    @Deprecated
    public UndertowHttpServer(String host, int port, Properties properties, HttpHandler handler) {

        super(host, port, properties, handler);
    }

    /**
     * Constructor.
     *
     * @param properties Server parameters.
     * @param handler    Root HTTP-handler.
     * @see HttpServer.Props
     * @see UndertowOptions
     */
    public UndertowHttpServer(Properties properties, HttpHandler handler) {
        super(properties, handler);
    }

    @Override
    protected void startInternal() {
        int connectionThreshold = PropertiesUtil.get(Props.CONNECTION_THRESHOLD, properties).get();
        final ExceptionHandler exceptionHandler = new ExceptionHandler(handler);

        Undertow.Builder builder = Undertow.builder()
                .addHttpListener(port, host)
                .setHandler(exchange -> exceptionHandler.handle(wrap(exchange)))
                .setSocketOption(Options.CONNECTION_HIGH_WATER, connectionThreshold)
                .setSocketOption(Options.CONNECTION_LOW_WATER, connectionThreshold);

        PropertiesUtil.get(Props.IO_THREADS, properties)
                .ifPresent(builder::setIoThreads);
        PropertiesUtil.get(Props.WORKER_THREADS, properties)
                .ifPresent(builder::setWorkerThreads);
        PropertiesUtil.get(UndertowProps.READ_TIMEOUT, properties)
                .ifPresent(value -> builder.setSocketOption(Options.READ_TIMEOUT, value));
        PropertiesUtil.get(UndertowProps.WRITE_TIMEOUT, properties)
                .ifPresent(value -> builder.setSocketOption(Options.WRITE_TIMEOUT, value));
        PropertiesUtil.get(UndertowProps.REQUEST_PARSE_TIMEOUT, properties)
                .ifPresent(value -> builder.setServerOption(UndertowOptions.REQUEST_PARSE_TIMEOUT, value));
        PropertiesUtil.get(UndertowProps.IDLE_TIMEOUT, properties)
                .ifPresent(value -> builder.setServerOption(UndertowOptions.IDLE_TIMEOUT, value));
        PropertiesUtil.get(UndertowProps.NO_REQUEST_TIMEOUT, properties)
                .ifPresent(value -> builder.setServerOption(UndertowOptions.NO_REQUEST_TIMEOUT, value));

        undertow = builder.build();

        undertow.start();
    }

    @Override
    protected boolean stopInternal(long timeout, TimeUnit unit) {
        undertow.stop();//TODO: replace with custom stopper with time quota
        return true;
    }

    private static HttpServerRequest wrap(HttpServerExchange exchange) {
        return new UndertowHttpServerRequest(exchange);
    }

    /**
     * Specific Undertow properties.
     */
    public static class UndertowProps {

        /**
         * Configure a read timeout for a socket, in milliseconds.
         *
         * If the given amount of time elapses without a successful read taking place, the socket's next read will throw a ReadTimeoutException.
         *
         * @see Options#READ_TIMEOUT
         */
        public static final Parameter<Integer> READ_TIMEOUT = Parameter.integerParameter("readTimeout")
                .build();

        /**
         * Configure a write timeout for a socket, in milliseconds.
         *
         * If the given amount of time elapses without a successful write taking place, the socket's next write will throw a WriteTimeoutException.
         *
         * @see Options#WRITE_TIMEOUT
         */
        public static final Parameter<Integer> WRITE_TIMEOUT = Parameter.integerParameter("writeTimeout")
                .build();

        /**
         * The maximum allowed time of reading HTTP request in milliseconds.
         *
         * -1 or missing value disables this functionality.
         *
         * @see UndertowOptions#REQUEST_PARSE_TIMEOUT
         */
        public static final Parameter<Integer> REQUEST_PARSE_TIMEOUT = Parameter.integerParameter("requestParseTimeout")
                .build();

        /**
         * The idle timeout in milliseconds after which the channel will be closed.
         *
         * If the underlying channel already has a read or write timeout set the smaller of the two values will be used for read/write timeouts.
         *
         * @see UndertowOptions#IDLE_TIMEOUT
         */
        public static final Parameter<Integer> IDLE_TIMEOUT = Parameter.integerParameter("idleTimeout")
                .build();

        /**
         * The amount of time the connection can be idle with no current requests before it is closed.
         *
         * @see UndertowOptions#NO_REQUEST_TIMEOUT
         */
        public static final Parameter<Integer> NO_REQUEST_TIMEOUT = Parameter.integerParameter("noRequestTimeout")
                .build();
    }
}
