package ru.kontur.vostok.hercules.util.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.util.net.LocalhostResolver;
import ru.kontur.vostok.hercules.util.properties.PropertyDescription;
import ru.kontur.vostok.hercules.util.properties.PropertyDescriptions;
import ru.kontur.vostok.hercules.util.throwable.ThrowableUtil;

import java.io.InputStream;
import java.util.Objects;
import java.util.Properties;

/**
 * ApplicationContextHolder stores context of application
 *
 * @author Kirill Sulim
 */
public class ApplicationContextHolder {

    private static class GitProps {
        static final PropertyDescription<String> COMMIT_ID = PropertyDescriptions
                .stringProperty("git.commit.id")
                .withDefaultValue("unknown")
                .build();

        static final PropertyDescription<String> VERSION = PropertyDescriptions
                .stringProperty("git.build.version")
                .withDefaultValue("unknown")
                .build();
    }

    private static class ContextProps {
        static final PropertyDescription<String> ENVIRONMENT = PropertyDescriptions
                .stringProperty("environment")
                .build();

        static final PropertyDescription<String> DATACENTER = PropertyDescriptions
                .stringProperty("datacenter")
                .build();

        static final PropertyDescription<String> INSTANCE_ID = PropertyDescriptions
                .stringProperty("instance.id")
                .build();
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationContextHolder.class);

    private static final String GIT_PROPERTIES = "git.properties";

    private static volatile ApplicationContext applicationContext;

    /**
     * Init application context. Must be called on startup if some parts of application uses this context
     * (such as MetricsCollector).
     *
     * @param applicationName human readable application name
     * @param applicationId application id without spaces and special symbols
     * @param contextProperties context properties
     */
    public static void init(
            String applicationName,
            String applicationId,
            Properties contextProperties
    ) {
        // Load git properties
        Properties gitProperties = loadGitProperties();
        final String commitId = GitProps.COMMIT_ID.extract(gitProperties);
        final String version = GitProps.VERSION.extract(gitProperties);

        // Load context properties
        final String environment = ContextProps.ENVIRONMENT.extract(contextProperties);
        final String dataCenter = ContextProps.DATACENTER.extract(contextProperties);
        final String instanceId = ContextProps.INSTANCE_ID.extract(contextProperties);

        // Load hostname
        final String hostname = LocalhostResolver.getLocalHostName();

        applicationContext = new ApplicationContext(
                applicationName,
                applicationId,
                version,
                commitId,
                environment,
                dataCenter,
                hostname,
                instanceId
        );
    }

    /**
     * Get application context
     *
     * @return application context
     */
    public static ApplicationContext get() {
        if (Objects.isNull(applicationContext)) {
            throw new IllegalStateException("Context is not initialized");
        }
        return applicationContext;
    }

    private static Properties loadGitProperties() {
        Properties gitProperties = new Properties();
        InputStream resourceAsStream = ApplicationContextHolder.class.getClassLoader().getResourceAsStream(GIT_PROPERTIES);
        if (Objects.nonNull(resourceAsStream)) {
            ThrowableUtil.toUnchecked(() -> gitProperties.load(resourceAsStream));
        } else {
            LOGGER.warn("Cannot load '{}' file", GIT_PROPERTIES);
        }
        return gitProperties;
    }
}
