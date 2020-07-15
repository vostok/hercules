package ru.kontur.vostok.hercules.json;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * JSON-document utility class.
 *
 * @author Gregory Koshelev
 */
public final class DocumentUtil {
    /**
     * Returns the sub document for the {@code path}.
     * <p>
     * Creates absent nodes in the source document if needed.
     *
     * @param document the JSON-document
     * @param path     the path in the JSON-document
     * @return the sub document
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> subdocument(Map<String, Object> document, List<String> path) {
        Map<String, Object> current = document;
        for (String parent : path) {
            Object obj = current.computeIfAbsent(parent, k -> new LinkedHashMap<>());
            if (!(obj instanceof Map)) {
                throw new IllegalArgumentException("Expected Map but got " + obj.getClass() + " for field " + parent);
            }
            current = (Map<String, Object>) obj;
        }
        return current;
    }

    private DocumentUtil() {
        /* static class */
    }
}
