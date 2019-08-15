package ru.kontur.vostok.hercules.configuration.util;

import ru.kontur.vostok.hercules.util.text.StringUtil;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * @author Gregory Koshelev
 */
public class PropertiesReader {
    public static Properties read(InputStream in) {
        Properties properties = new Properties();
        try {
            properties.load(in);
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot read properties", ex);
        }
        return properties;
    }
}
