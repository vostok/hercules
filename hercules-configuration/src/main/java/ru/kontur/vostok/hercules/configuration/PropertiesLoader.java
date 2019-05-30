package ru.kontur.vostok.hercules.configuration;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.configuration.file.FilePropertiesSource;
import ru.kontur.vostok.hercules.configuration.zk.ZkPropertiesSource;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Utility class to load properties from different sources
 *
 * @author Gregory Koshelev
 */
public final class PropertiesLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(PropertiesLoader.class);

    private static final Map<String, PropertiesSource> SOURCES;

    static {
        Map<String, PropertiesSource> sources = new HashMap<>();

        sources.put("file", new FilePropertiesSource());
        sources.put("zk", new ZkPropertiesSource());
        sources.put("resource", new ResourcePropertiesSource());

        SOURCES = sources;
    }

    /**
     * Load properties from {@code source} path.
     * <p>
     * Each source path should be started with the schema prefix like this: {@code '<schema>://'}.  <br>
     * Supported schemas:                                                                           <br>
     * {@code file} - load properties from file                                                     <br>
     * {@code zk} - load properties from ZooKeeper node                                             <br>
     * {@code resource} - load properties from resource file.                                       <br>
     *
     * @param source the source path with the schema prefix
     * @param throwOnError rethrow any exception if {@code true}
     * @return loaded properties or empty (if throwOnError is false)
     * @throws IllegalArgumentException if invalid or unknown schema in source path
     * @throws RuntimeException if it is thrown by {@link PropertiesSource}
     */
    public static Properties load(@NotNull String source, boolean throwOnError) {
        try {
            String schema = getSchemaFromSource(source);
            PropertiesSource propertiesSource = SOURCES.get(schema);
            if (propertiesSource == null) {
                throw new IllegalArgumentException("Unknown schema " + schema);
            }
            return propertiesSource.load(source);
        } catch (Exception ex) {
            LOGGER.error("Properties loading failed with exception", ex);
            if (throwOnError) throw ex;
            return new Properties();
        }
    }

    /**
     * Load properties from {@code source} path.
     * <p>
     * Each source path should be started with the schema prefix like this: {@code '<schema>://'}.  <br>
     * Supported schemas:                                                                           <br>
     * {@code file} - load properties from file                                                     <br>
     * {@code zk} - load properties from ZooKeeper node                                             <br>
     * {@code resource} - load properties from resource file.                                       <br>
     *
     * @param source the source path with the schema prefix
     * @return loaded properties
     * @throws IllegalArgumentException if invalid or unknown schema in source path
     * @throws RuntimeException if it is thrown by {@link PropertiesSource}
     */
    public static Properties load(@NotNull String source) {
        return load(source, true);
    }

    private static String getSchemaFromSource(String source) {
        int schemaLength = source.indexOf(':');
        if (schemaLength == -1) {
            throw new IllegalArgumentException("Invalid source string");
        }
        return source.substring(0, schemaLength).toLowerCase();
    }

    private PropertiesLoader() {
        /* static class */
    }
}
