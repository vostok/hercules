package ru.kontur.vostok.hercules.application;

/**
 * Default values of {@link ApplicationConfig} properties.
 *
 * @author Gregory Koshelev
 */
public final class ApplicationConfigDefaults {
    /**
     * Default host of HTTP-server.
     */
    public static final String DEFAULT_HOST = "0.0.0.0";

    /**
     * Default port of HTTP-server.
     */
    public static final int DEFAULT_PORT = 8080;

    /**
     * Default shutdown timeout in millis.
     * <p>
     * Timeout to stop all application components.
     */
    public static final int DEFAULT_SHUTDOWN_TIMEOUT_MS = 5_000;

    /**
     * Default shutdown grace period in millis.
     * <p>
     * Period before stopping application components.
     */
    public static final int DEFAULT_SHUTDOWN_GRACE_PERIOD_MS = 0;

    private ApplicationConfigDefaults() {
        /* static class */
    }
}
