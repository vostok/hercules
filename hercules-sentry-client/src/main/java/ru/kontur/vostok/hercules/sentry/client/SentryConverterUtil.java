package ru.kontur.vostok.hercules.sentry.client;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.TinyString;
import ru.kontur.vostok.hercules.protocol.Type;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.protocol.Vector;

/**
 * Utility class for SentryConverter
 *
 * @author Petr Demenev
 */
public class SentryConverterUtil {

    private static final int MAX_TAG_LENGTH = 200;
    private static final String DELIMITER = ".";

    /**
     * Try to resolve platform by file extension
     * @param fileName name of file with extension
     * @return platform name or empty
     */
    public static Optional<String> resolvePlatformByFileName(final String fileName) {
        if (Objects.isNull(fileName)) {
            return Optional.empty();
        }

        final String lowerCaseFileName = fileName.toLowerCase();

        if (lowerCaseFileName.endsWith(".java")) {
            return Optional.of("java");
        } else if (lowerCaseFileName.endsWith(".cs")) {
            return Optional.of("csharp");
        } else if (lowerCaseFileName.endsWith(".py")) {
            return Optional.of("python");
        } else {
            return Optional.empty();
        }
    }

    /**
     * Extract sentryObject from initial value
     * @param variant initial value
     * @return extracted value
     */
    public static Object extractObject(Variant variant) {
        return asSentryObject(variant.getType(), variant.getValue());
    }

    /**
     * Extract string from initial value
     * @param variant initial value
     * @return extracted string
     */
    public static String extractString(Variant variant) {
        if (variant.getType() == Type.STRING) {
            return new String((byte[]) variant.getValue(), StandardCharsets.UTF_8);
        } else {
            return String.valueOf(variant.getValue());
        }
    }

    /**
     * Transform string value to acceptable value for Sentry tag
     * @param value initial value
     * @return transformed string value
     */
    public static String sanitizeTagValue(Variant value) {
        String stringValue = extractString(value).trim();
        if (stringValue.length() > MAX_TAG_LENGTH) {
            stringValue = stringValue.substring(0, MAX_TAG_LENGTH);
        } else if (stringValue.length() == 0) {
            stringValue = "[empty]";
        }
        return stringValue.replace('\n', ' ');
    }

    /**
     * Try to cut prefix from string
     * @param prefix prefix
     * @param sourceName original string
     * @return string without prefix or empty (if source string not starts with prefix)
     */
    public static Optional<String> cutOffPrefixIfExists(String prefix, String sourceName) {
        final String prefixWithDelimiter = prefix + DELIMITER;
        if (sourceName.length() <= prefixWithDelimiter.length()) {
            return Optional.empty();
        }
        if (!sourceName.startsWith(prefixWithDelimiter)) {
            return Optional.empty();
        }
        return Optional.of(sourceName.substring(prefixWithDelimiter.length()));
    }

    private static Object asSentryObject(Type type, Object object) {
        switch (type) {
            case STRING:
                return new String((byte[]) object, StandardCharsets.UTF_8);
            case CONTAINER:
                Map<String, Object> map = new HashMap<>();
                for (Map.Entry<TinyString, Variant> entry : ((Container) object).tags().entrySet()) {
                    map.put(entry.getKey().toString(), extractObject(entry.getValue()));
                }
                return map;
            case VECTOR:
                Vector vector = (Vector) object;
                return extractFromVector(vector);
            case NULL:
                return "null";
            default:
                return object;
        }
    }

    private SentryConverterUtil() {
        /* static class */
    }

    private static List<Object> extractFromVector(Vector vector) {
        List<Object> resultList = new ArrayList<>();
        switch (vector.getType()) {
            case BYTE:
                byte[] bytes = (byte[]) vector.getValue();
                for (byte element : bytes) {
                    resultList.add(element);
                }
                break;
            case SHORT:
                short[] shorts = (short[]) vector.getValue();
                for (short element : shorts) {
                    resultList.add(element);
                }
                break;
            case INTEGER:
                int[] ints = (int[]) vector.getValue();
                for (int element : ints) {
                    resultList.add(element);
                }
                break;
            case LONG:
                long[] longs = (long[]) vector.getValue();
                for (long element : longs) {
                    resultList.add(element);
                }
                break;
            case FLAG:
                boolean[] flags = (boolean[]) vector.getValue();
                for (boolean element : flags) {
                    resultList.add(element);
                }
                break;
            case FLOAT:
                float[] floats = (float[]) vector.getValue();
                for (float element : floats) {
                    resultList.add(element);
                }
                break;
            case DOUBLE:
                double[] doubles = (double[]) vector.getValue();
                for (double element : doubles) {
                    resultList.add(element);
                }
                break;
            default:
                Object[] objects = (Object[]) vector.getValue();
                for (Object element : objects) {
                    resultList.add(asSentryObject(vector.getType(), element));
                }
        }
        return resultList;
    }
}
