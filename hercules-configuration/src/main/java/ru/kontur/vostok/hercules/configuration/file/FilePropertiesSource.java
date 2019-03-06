package ru.kontur.vostok.hercules.configuration.file;

import org.jetbrains.annotations.NotNull;
import ru.kontur.vostok.hercules.configuration.PropertiesSource;
import ru.kontur.vostok.hercules.configuration.util.PropertiesReader;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * @author Gregory Koshelev
 */
public class FilePropertiesSource implements PropertiesSource {
    @Override
    public Properties load(@NotNull String source) {
        if (!source.startsWith("file://")) {
            throw new IllegalArgumentException("Source should started with 'file://");
        }

        try (InputStream in = new FileInputStream(source.substring("file://".length()))) {
            return PropertiesReader.read(in);
        } catch (FileNotFoundException ex) {
            throw new IllegalArgumentException("Properties file not found", ex);
        } catch (IOException ex) {
            throw new IllegalStateException("Exception when read properties", ex);
        }

    }
}
