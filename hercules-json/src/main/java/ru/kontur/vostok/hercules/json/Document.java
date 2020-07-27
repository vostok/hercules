package ru.kontur.vostok.hercules.json;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * JSON-document
 * <p>
 * The JSON-document is represented by an object model of nested {@link Map} objects.<br>
 * A key of an entry is the tag name.
 * A value of an entry is one of following types:
 * <ul>
 *   <li>Boxed primitive,
 *   <li>{@link String},
 *   <li>arrays of primitives,
 *   <li>{@link java.util.List} of {@link String} or {@link Map}
 *   <li>{@link Map}
 * </ul>
 *
 * @author Gregory Koshelev
 */
public class Document {
    private final Map<String, Object> document;

    public Document() {
        this.document = new LinkedHashMap<>();
    }

    private Document(Map<String, Object> document) {
        this.document = document;
    }

    /**
     * Puts the field to the document if absent.
     *
     * @param field the field name
     * @param value the value
     */
    public void putIfAbsent(String field, Object value) {
        document.putIfAbsent(field, value);
    }

    /**
     * Returns the sub document node for the {@code path}.
     * <p>
     * Creates absent nodes in the source document if needed.
     *
     * @param path the path in the JSON-document
     * @return the sub document node
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> subdocument(List<String> path) {
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

    Map<String, Object> document() {
        return document;
    }

    /**
     * Creates the document from {@link Map}.
     *
     * @param map the compatible object model
     * @return the document
     */
    public static Document of(Map<String, Object> map) {
        return new Document(new LinkedHashMap<>(map));
    }
}
