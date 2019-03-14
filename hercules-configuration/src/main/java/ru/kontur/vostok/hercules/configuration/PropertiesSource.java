package ru.kontur.vostok.hercules.configuration;

import org.jetbrains.annotations.NotNull;

import java.util.Properties;

/**
 * @author Gregory Koshelev
 */
public interface PropertiesSource {
    Properties load(@NotNull String source);
}
