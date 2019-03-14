package ru.kontur.vostok.hercules.configuration;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.configuration.file.FilePropertiesSource;
import ru.kontur.vostok.hercules.configuration.zk.ZkPropertiesSource;
import ru.kontur.vostok.hercules.util.Maps;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * @author Gregory Koshelev
 */
public class PropertiesLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(PropertiesLoader.class);

    private static final Map<String, PropertiesSource> SOURCES;

    static {
        Map<String, PropertiesSource> sources = new HashMap<>();

        sources.put("file", new FilePropertiesSource());
        sources.put("zk", new ZkPropertiesSource());

        SOURCES = sources;
    }

    public static Properties load(@NotNull String source) {
        try {
            String schema = getSchemaFromSource(source);
            PropertiesSource propertiesSource = SOURCES.get(schema);
            if (propertiesSource == null) {
                throw new IllegalArgumentException("Unknown schema " + schema);
            }
            return propertiesSource.load(source);
        } catch (Exception ex) {
            LOGGER.error("Properties loading failed with exception", ex);
            throw ex;
        }
    }

    private static String getSchemaFromSource(String source) {
        int schemaLength = source.indexOf(':');
        if (schemaLength == -1) {
            throw new IllegalArgumentException("Invalid source string");
        }
        return source.substring(0, schemaLength).toLowerCase();
    }
}
