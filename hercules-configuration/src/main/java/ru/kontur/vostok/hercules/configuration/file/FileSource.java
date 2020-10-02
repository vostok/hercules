package ru.kontur.vostok.hercules.configuration.file;

import org.jetbrains.annotations.NotNull;
import ru.kontur.vostok.hercules.configuration.Source;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

/**
 * @author Gregory Koshelev
 */
public class FileSource implements Source {
    @Override
    public InputStream load(@NotNull String sourcePath) {
        if (!sourcePath.startsWith("file://")) {
            throw new IllegalArgumentException("Source should started with 'file://");
        }

        try {
            return new FileInputStream(sourcePath.substring("file://".length()));
        } catch (FileNotFoundException ex) {
            throw new IllegalArgumentException("File not found", ex);
        }
    }
}
