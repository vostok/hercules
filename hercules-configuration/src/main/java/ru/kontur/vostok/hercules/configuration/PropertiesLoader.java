package ru.kontur.vostok.hercules.configuration;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.configuration.util.PropertiesReader;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Utility class to load properties from different sources
 *
 * @author Gregory Koshelev
 */
public final class PropertiesLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(PropertiesLoader.class);

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
     * @throws IllegalStateException if resource has not been closed properly
     * @throws RuntimeException if it is thrown by {@link Source}
     */
    public static Properties load(@NotNull String source, boolean throwOnError) {
        try {
            try (InputStream in = Sources.load(source)) {
                return PropertiesReader.read(in);
            } catch (IOException ex) {
                throw new IllegalStateException("Exception when closing stream", ex);
            }
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
     * @throws RuntimeException if it is thrown by {@link Source}
     */
    public static Properties load(@NotNull String source) {
        return load(source, true);
    }

    private PropertiesLoader() {
        /* static class */
    }
}
