package ru.kontur.vostok.hercules.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.configuration.supplier.PropertiesSupplier;
import ru.kontur.vostok.hercules.configuration.util.ArgsParser;
import ru.kontur.vostok.hercules.util.net.LocalhostResolver;
import ru.kontur.vostok.hercules.util.time.TimeSource;
import ru.kontur.vostok.hercules.util.time.TimeSource.Result;

import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

/**
 * Application controller class.
 *
 * @author Gregory Koshelev
 */
public final class Application {

    private static volatile Application application;

    private final Container container;
    private final ApplicationConfig config;
    private final ApplicationContext context;
    private final Logger logger;
    private final TimeSource timeSource;
    private final CopyOnWriteArrayList<ApplicationStateObserver> applicationStateObservers = new CopyOnWriteArrayList<>();
    private volatile ApplicationState state = ApplicationState.INIT;
    private Thread shutdownHook;

    Application(
            ApplicationConfig config,
            ApplicationContext context,
            Container container,
            Logger logger,
            TimeSource timeSource
    ) {
        this.config = config;
        this.context = context;
        this.container = container;
        this.logger = logger;
        this.timeSource = timeSource;

        var stateObservers = this.config.getStateObservers();
        if (stateObservers != null) {
            stateObservers.forEach(this::addStateObserver);
        }
    }

    /**
     * Run the Hercules application.
     *
     * @param runner Hercules application runner.
     * @param args   Command line arguments.
     * @return Created application.
     */
    public static Application run(ApplicationRunner runner, String... args) {
        if (application != null) {
            application.expectStopped();
        }
        var logger = LoggerFactory.getLogger(Application.class);
        boolean result;
        try {
            var cmdArguments = ArgsParser.parse(args);
            var properties = PropertiesSupplier.defaultSupplier(cmdArguments).get();
            var config = new ApplicationConfig(properties);
            var contextConfig = new ApplicationContextConfig(properties);
            var context = new ApplicationContext(
                    runner.getApplicationName(),
                    runner.getApplicationId(),
                    Version.get().getVersion(),
                    Version.get().getCommitId(),
                    contextConfig.getEnvironment(),
                    contextConfig.getZone(),
                    contextConfig.getInstanceId(),
                    LocalhostResolver.getLocalHostName());
            application = new Application(config, context, new Container(), logger, TimeSource.SYSTEM);
            result = application.start(runner);
        } catch (Exception ex) {
            logger.error("Unexpected error occurred while starting application", ex);
            result = false;
        }
        if (!result) {
            System.exit(1);
        }
        return application;
    }

    /**
     * Run application.
     *
     * @see Application#run(ApplicationRunner, String[])
     * @deprecated use {@link Application#run(ApplicationRunner, String[])} instead
     */
    @Deprecated
    public static void run(String applicationName, String applicationId, String[] args) {
        run(applicationName, applicationId, args, (properties, container) -> {
        });
    }

    /**
     * Run application.
     *
     * @see Application#run(ApplicationRunner, String[])
     * @deprecated use {@link Application#run(ApplicationRunner, String[])} instead
     */
    @Deprecated
    public static void run(String applicationName, String applicationId, String[] args, Starter starter) {
        var runner = new ApplicationRunner() {
            @Override
            public String getApplicationId() {
                return applicationId;
            }

            @Override
            public String getApplicationName() {
                return applicationName;
            }

            @Override
            public void init(Application application) {
                starter.start(application.getConfig().getAllProperties(), application.getContainer());
            }
        };
        run(runner, args);
    }

    /**
     * Get execution context of current application.
     *
     * @return Context of current application.
     */
    public static ApplicationContext context() {
        return application().context;
    }

    /**
     * Get current application.
     *
     * @return Current application.
     */
    public static Application application() {
        return application;
    }

    public Container getContainer() {
        return container;
    }

    public ApplicationConfig getConfig() {
        return config;
    }

    public ApplicationState getState() {
        return state;
    }

    public ApplicationContext getContext() {
        return context;
    }

    /**
     * Add {@link ApplicationStateObserver} to the subscription list.
     *
     * @param observer New observer.
     */
    public void addStateObserver(ApplicationStateObserver observer) {
        applicationStateObservers.add(observer);
    }

    /**
     * Start application using given {@link ApplicationRunner runner}.
     *
     * @param runner {@link ApplicationRunner} that instantiate concrete application.
     * @return {@code true} if startup process were successful, or {@code false} otherwise.
     */
    public synchronized boolean start(ApplicationRunner runner) {
        if (state != ApplicationState.INIT && state != ApplicationState.STOPPED) {
            throw new IllegalStateException("can start application only in " + ApplicationState.INIT + " or "
                    + ApplicationState.STOPPED + " but current state was " + state);
        }

        try {
            long initDurationMs = timeSource.measureMs(() -> {
                changeState(ApplicationState.STARTING);
                runner.init(this);
            });
            logger.info("Application {} initialized for {} millis", context.getApplicationName(), initDurationMs);
            changeState(ApplicationState.RUNNING);
            shutdownHook = new Thread(this::stop);
            Runtime.getRuntime().addShutdownHook(shutdownHook);
            return true;
        } catch (Exception ex) {
            logger.error("Fatal error occurred during application initialization", ex);
            stop();
            return false;
        }
    }

    /**
     * Stop the application.
     */
    public synchronized void stop() {
        if (state != ApplicationState.STARTING && state != ApplicationState.RUNNING) {
            return;
        }
        try {
            Result<Boolean> hardResult = timeSource.measureMs(() -> {
                changeState(ApplicationState.STOPPING);
                timeSource.sleep(config.getShutdownGracePeriodMs());
                logger.info("Started {} shutdown process", context.getApplicationName());
                return container.stop(config.getShutdownTimeoutMs(), TimeUnit.MILLISECONDS);
            });
            logger.info("Finished {} shutting down for {} millis with status {}",
                    context.getApplicationName(), hardResult.getDurationMs(), (hardResult.getValue() ? "OK" : "FAILED"));
            changeState(ApplicationState.STOPPED);
        } finally {
            if (shutdownHook != null) {
                try {
                    Runtime.getRuntime().removeShutdownHook(shutdownHook);
                } catch (IllegalStateException ignore) {
                    // VM already shutting down
                }
                shutdownHook = null;
            }
        }
    }

    private void expectStopped() {
        var currentState = state;
        if (currentState != ApplicationState.STOPPED) {
            throw new IllegalStateException("Expect state " + ApplicationState.STOPPED + ", but was " + currentState);
        }
    }

    private void changeState(ApplicationState newState) {
        this.state = newState;
        notifyState();
    }

    private void notifyState() {
        for (ApplicationStateObserver observer : applicationStateObservers) {
            try {
                observer.onApplicationStateChanged(this, state);
            } catch (Exception ex) {
                logger.error("Application state observer {} thrown exception during state observers notification", observer, ex);
            }
        }
    }

    /**
     * Interface for application context configuration methods.
     *
     * @deprecated Use {@link ApplicationRunner} instead.
     */
    @Deprecated
    public interface Starter {

        void start(Properties properties, Container container);
    }
}
