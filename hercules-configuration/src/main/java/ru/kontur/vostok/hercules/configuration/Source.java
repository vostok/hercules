package ru.kontur.vostok.hercules.configuration;

import org.jetbrains.annotations.NotNull;

import java.io.InputStream;

/**
 * @author Gregory Koshelev
 */
public interface Source {
    InputStream load(@NotNull String sourcePath);
}
