package ru.kontur.vostok.hercules.application;

import ru.kontur.vostok.hercules.configuration.PropertiesLoader;
import ru.kontur.vostok.hercules.configuration.Scopes;
import ru.kontur.vostok.hercules.configuration.util.ArgsParser;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;
import ru.kontur.vostok.hercules.util.net.LocalhostResolver;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Gregory Koshelev
 */
public class Application {
    private static volatile Application application;
    private static AtomicReference<ApplicationState> state = new AtomicReference<>(ApplicationState.INIT);

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

    public static void run(String applicationName, String applicationId, String[] args) {
        expectState(ApplicationState.INIT);

        Map<String, String> parameters = ArgsParser.parse(args);
        Properties properties = PropertiesLoader.load(parameters.getOrDefault("application.properties", "file://application.properties"));

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

        //TODO: run all components

        //TODO: shutdown hook is required
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
}
