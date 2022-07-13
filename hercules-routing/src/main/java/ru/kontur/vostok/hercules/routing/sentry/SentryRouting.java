package ru.kontur.vostok.hercules.routing.sentry;

import ru.kontur.vostok.hercules.routing.engine.tree.DecisionTreeEngineConfig;

/**
 * Common constants for Sentry event routing.
 *
 * @author Aleksandr Yuferov
 */
public class SentryRouting {
    /**
     * ZNode path in ZooKeeper where configuration is stored.
     */
    public static final String STORE_ROOT = "/hercules/routing/routes/sink/sentry";

    /**
     * Default configuration of decision tree engine.
     */
    public static final DecisionTreeEngineConfig DEFAULT_CONFIG = DecisionTreeEngineConfig.builder()
            .addAllowedTag("properties/project")
            .addAllowedTag("properties/subproject")
            .addAllowedTag("properties/application")
            .addAllowedTag("properties/environment")
            .build();

    private SentryRouting() {
    }
}
