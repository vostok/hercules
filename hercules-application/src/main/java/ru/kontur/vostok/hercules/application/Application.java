package ru.kontur.vostok.hercules.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.configuration.PropertiesLoader;
import ru.kontur.vostok.hercules.configuration.Scopes;
import ru.kontur.vostok.hercules.configuration.util.ArgsParser;
import ru.kontur.vostok.hercules.util.lifecycle.Lifecycle;
import ru.kontur.vostok.hercules.util.lifecycle.Stoppable;
import ru.kontur.vostok.hercules.util.net.LocalhostResolver;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;
import ru.kontur.vostok.hercules.util.time.TimeSource;
import ru.kontur.vostok.hercules.util.time.Timer;

import java.io.Closeable;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Gregory Koshelev
 */
public class Application {
    private static final Logger LOGGER = LoggerFactory.getLogger(Application.class);

    private static volatile Application application;
    private static AtomicReference<ApplicationState> state = new AtomicReference<>(ApplicationState.INIT);
    private static final Container container = new Container();

    private final String applicationName;
    private final String applicationId;
    private final ApplicationConfig config;
    private final ApplicationContext context;

    private Application(String applicationName, String applicationId, ApplicationConfig config, ApplicationContext context) {
        this.applicationName = applicationName;
        this.applicationId = applicationId;
        this.config = config;
        this.context = context;
    }

    /**
     * @deprecated use {@link #run(String, String, String[], Starter)} instead
     */
    @Deprecated
    public static void run(String applicationName, String applicationId, String[] args) {
        run(applicationName, applicationId, args, (properties, container) -> {
        });
    }

    public static void run(String applicationName, String applicationId, String[] args, Starter starter) {
        expectState(ApplicationState.INIT);
        long start = System.currentTimeMillis();
        LOGGER.info("Starting application {}", applicationName);

        Map<String, String> parameters = ArgsParser.parse(args);
        Properties properties = PropertiesLoader.load(parameters.getOrDefault("application.properties", "file://application.properties"));
        LOGGER.info(PropertiesUtil.prettyView(properties));

        ApplicationConfig applicationConfig = new ApplicationConfig(PropertiesUtil.ofScope(properties, Scopes.APPLICATION));

        ApplicationContextConfig applicationContextConfig = new ApplicationContextConfig(PropertiesUtil.ofScope(properties, Scopes.CONTEXT));
        ApplicationContext context = new ApplicationContext(
                applicationName,
                applicationId,
                Version.get().getVersion(),
                Version.get().getCommitId(),
                applicationContextConfig.getEnvironment(),
                applicationContextConfig.getZone(),
                applicationContextConfig.getInstanceId(),
                LocalhostResolver.getLocalHostName());

        Application.application = new Application(applicationName, applicationId, applicationConfig, context);

        changeState(ApplicationState.INIT, ApplicationState.STARTING);

        try {
            starter.start(properties, container);
        } catch (Throwable t) {
            LOGGER.error("Cannot start application due to", t);
            shutdown();
            System.exit(1);
        }

        Runtime.getRuntime().addShutdownHook(new Thread(Application::shutdown));

        LOGGER.info(
                "Application {} started for {} millis",
                applicationName,
                System.currentTimeMillis() - start);
        changeState(ApplicationState.STARTING, ApplicationState.RUNNING);
    }

    private static void shutdown() {
        long start = System.currentTimeMillis();

        LOGGER.info("Started {} application shutdown process", application.applicationName);
        changeState(ApplicationState.STOPPING);

        boolean stopped = container.stop(5_000, TimeUnit.MILLISECONDS);

        LOGGER.info(
                "Finished {} shutdown for {} millis with status {}",
                application.applicationName,
                System.currentTimeMillis() - start,
                (stopped ? "OK" : "FAILED"));
        changeState(ApplicationState.STOPPING, ApplicationState.STOPPED);
    }

    public static ApplicationContext context() {
        expectAtLeastState(ApplicationState.STARTING);

        return application.context;
    }

    public static Application application() {
        expectAtLeastState(ApplicationState.STARTING);

        return application;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public ApplicationConfig getConfig() {
        return config;
    }

    private static void expectState(ApplicationState expected) {
        ApplicationState current = state.get();
        if (current == expected) {
            return;
        }
        throw new IllegalStateException("Expect state " + expected + ", but was " + current);
    }

    private static void changeState(ApplicationState expected, ApplicationState newState) {
        if (state.compareAndSet(expected, newState)) {
            return;
        }
        throw new IllegalStateException("Expect state " + expected + ", but was " + state.get());
    }

    private static void expectAtLeastState(ApplicationState expected) {
        ApplicationState current = state.get();
        if (current.order() >= expected.order()) {
            return;
        }
        throw new IllegalStateException("Expect at least state " + expected + ", but was " + current);
    }

    private static void changeState(ApplicationState newState) {
        state.set(newState);
    }

    /**
     * Container registers components
     * and provides graceful shutdown in a reversed order (LIFO fashion).
     */
    public static class Container {
        private final ConcurrentLinkedDeque<Stoppable> components = new ConcurrentLinkedDeque<>();

        /**
         * Register and start the component.
         * <p>
         * Registered component will properly stopped on application shutdown in an appropriate moment.
         * Components are stopped in LIFO fashion.
         *
         * @param component the component
         * @param <T>       implements {@link Lifecycle}
         * @return the component
         * @see #register(Stoppable)
         */
        public <T extends Lifecycle> T register(T component) {
            components.push(component);
            component.start();
            return component;
        }

        /**
         * Register the component.
         * <p>
         * Registered component will properly stopped on application shutdown in an appropriate moment.
         * Components are stopped in LIFO fashion.
         *
         * @param component the component
         * @param <T>       implements {@link Stoppable}
         * @return the component
         * @see #register(Lifecycle)
         */
        public <T extends Stoppable> T register(T component) {
            components.push(component);
            return component;
        }

        /**
         * Register the component.
         * <p>
         * Registered component will properly closed on application shutdown in an appropriate moment.
         * Components are stopped in LIFO fashion.
         *
         * @param component the component
         * @param <T>       implements Closeable
         * @return the component
         */
        public <T extends Closeable> T register(T component) {
            components.push(Stoppable.from(component));
            return component;
        }

        /**
         * Stop components one-by-one in LIFO fashion.
         * <p>
         * The method is package-private to avoid possible bugs when it is called externally.
         *
         * @param timeout the timeout
         * @param unit    the time unit
         * @return {@code true} if successfully stopped all components
         */
        boolean stop(long timeout, TimeUnit unit) {
            Timer timer = TimeSource.SYSTEM.timer(unit.toMillis(timeout));
            boolean stopped = true;
            for (Stoppable component : components) {
                try {
                    boolean result = component.stop(timer.remainingTimeMs(), TimeUnit.MILLISECONDS);
                    if (!result) {
                        LOGGER.warn("Component " + component.getClass() + " has not been stopped properly");
                    }
                    stopped = stopped && result;
                } catch (Throwable t) {
                    LOGGER.error("Error on stopping " + component.getClass(), t);
                    stopped = false;
                }
            }
            return stopped;
        }
    }

    public interface Starter {
        void start(Properties properties, Container container);
    }
}
