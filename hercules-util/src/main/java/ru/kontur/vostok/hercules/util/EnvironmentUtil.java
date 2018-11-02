package ru.kontur.vostok.hercules.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * EnvironmentUtil
 *
 * @author Kirill Sulim
 */
public final class EnvironmentUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentUtil.class);

    public static String getEnvSafe(String envName) {
        String envValue = null;
        try {
            envValue = System.getenv(envName);
        }
        catch (SecurityException e) {
            LOGGER.warn("Cannot get access to '{}' env", envName, e);
        }
        return envValue;
    }

    private EnvironmentUtil() {
        /* static class */
    }
}
