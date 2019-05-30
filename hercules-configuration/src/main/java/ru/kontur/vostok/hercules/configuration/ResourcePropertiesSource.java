package ru.kontur.vostok.hercules.configuration;

import org.jetbrains.annotations.NotNull;
import ru.kontur.vostok.hercules.configuration.util.PropertiesReader;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * @author Gregory Koshelev
 */
public class ResourcePropertiesSource implements PropertiesSource {
    @Override
    public Properties load(@NotNull String source) {
        if (!source.startsWith("resource://")) {
            throw new IllegalArgumentException("Source should started with 'resource://'");
        }

        try (InputStream in = ResourcePropertiesSource.class.getClassLoader().getResourceAsStream(
                source.substring("resource://".length()))) {
            if (in == null) {
                throw new IllegalArgumentException("Properties file not found");
            }
            return PropertiesReader.read(in);
        } catch (IOException ex) {
            throw new IllegalStateException("Exception when read properties", ex);
        }
    }
}
