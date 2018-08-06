package ru.kontur.vostok.hercules.micrometer.registry;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Properties;
import ru.kontur.vostok.hercules.util.properties.ConfigsUtil;

/**
 * @author Daniil Zhenikhov
 */
public class HerculesMetricConfigFactory {
    private static final String DEFAULT_RESOURCE_NAME = "hercules-metric.properties";
    private static final String PROPERTY_NAME = "hercules.metric.config";

    private static final String DEFAULT_CONFIG_PATH;
    private static final boolean DEFAULT_CONFIG_PATH_EXIST;

    static {
        DEFAULT_CONFIG_PATH = System.getProperty(PROPERTY_NAME);
        DEFAULT_CONFIG_PATH_EXIST = Objects.nonNull(DEFAULT_CONFIG_PATH);
    }

    /**
     * Create {@link ru.kontur.vostok.hercules.micrometer.registry.HerculesMetricConfig HerculesMetricConfig} from resource folder
     * with name <code>resourceName</code>
     *
     * @param resourceName name of resource containing configs
     * @return {@link ru.kontur.vostok.hercules.micrometer.registry.HerculesMetricConfig HerculesMetricConfig}
     * @throws IOException exception while loading config
     */
    public static HerculesMetricConfig fromResource(String resourceName) throws IOException {
        InputStream inputStream = ConfigsUtil.readConfig(resourceName, true);
        Properties properties = loadProperties(inputStream);

        return properties::getProperty;
    }

    /**
     * Create {@link ru.kontur.vostok.hercules.micrometer.registry.HerculesMetricConfig HerculesMetricConfig} from resource folder
     * with name {@value DEFAULT_RESOURCE_NAME}
     *
     * @return {@link ru.kontur.vostok.hercules.micrometer.registry.HerculesMetricConfig HerculesMetricConfig}
     * @throws IOException exception while loading config
     */
    public static HerculesMetricConfig fromResource() throws IOException {
        return fromResource(DEFAULT_RESOURCE_NAME);
    }

    /**
     * Create {@link ru.kontur.vostok.hercules.micrometer.registry.HerculesMetricConfig HerculesMetricConfig}
     * from specific file
     * with name <code>filename</code>
     *
     * @return {@link ru.kontur.vostok.hercules.micrometer.registry.HerculesMetricConfig HerculesMetricConfig}
     * @throws IOException exception while loading config
     */
    public static HerculesMetricConfig fromFile(String filename) throws IOException {
        InputStream inputStream = ConfigsUtil.readConfig(filename, false);
        Properties properties = loadProperties(inputStream);

        return properties::getProperty;
    }

    /**
     * Create {@link ru.kontur.vostok.hercules.micrometer.registry.HerculesMetricConfig HerculesMetricConfig}
     * from specific file. Path to file defined in system property {@value PROPERTY_NAME}
     *
     * @return {@link ru.kontur.vostok.hercules.micrometer.registry.HerculesMetricConfig HerculesMetricConfig}
     * @throws IOException exception while loading config
     */
    public static HerculesMetricConfig fromFile() throws IOException {
        if (DEFAULT_CONFIG_PATH_EXIST) {
            return fromFile(DEFAULT_CONFIG_PATH);
        }
        throw new FileNotFoundException(String.format("File '%s' has not found", DEFAULT_CONFIG_PATH));
    }

    private static Properties loadProperties(InputStream inputStream) throws IOException {
        Properties properties = new Properties();
        properties.load(inputStream);
        return properties;
    }
}
