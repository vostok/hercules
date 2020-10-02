package ru.kontur.vostok.hercules.configuration.resource;

import org.jetbrains.annotations.NotNull;
import ru.kontur.vostok.hercules.configuration.Source;

import java.io.InputStream;

/**
 * @author Gregory Koshelev
 */
public class ResourceSource implements Source {
    @Override
    public InputStream load(@NotNull String sourcePath) {
        if (!sourcePath.startsWith("resource://")) {
            throw new IllegalArgumentException("Source should started with 'resource://'");
        }

        InputStream in = ResourceSource.class.getClassLoader().getResourceAsStream(
                sourcePath.substring("resource://".length()));
        if (in == null) {
            throw new IllegalArgumentException("File not found");
        }
        return in;
    }
}
