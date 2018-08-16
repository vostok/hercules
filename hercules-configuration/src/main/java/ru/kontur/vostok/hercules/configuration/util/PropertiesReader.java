package ru.kontur.vostok.hercules.configuration.util;

import ru.kontur.vostok.hercules.util.text.StringUtil;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * @author Gregory Koshelev
 */
public class PropertiesReader {
    public static Properties read(String path) {
        if (StringUtil.isNullOrEmpty(path)) {
            throw new IllegalArgumentException("Cannot read properties: Invalid path");
        }

        try(InputStream in = new FileInputStream(path)) {
            Properties properties = new Properties();
            properties.load(in);
            return properties;
        } catch (IOException ex) {
            throw new IllegalArgumentException("Cannot read properties", ex);
        }
    }
}
