package ru.kontur.vostok.hercules.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

/**
 * Writes the JSON-document to the {@link OutputStream}.
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
public final class DocumentWriter {
    private static final ObjectWriter OBJECT_WRITER;

    static {
        ObjectMapper objectMapper = new ObjectMapper();

        OBJECT_WRITER = objectMapper.writerFor(Map.class);
    }

    /**
     * Writes the JSON-document to the {@link OutputStream}.
     *
     * @param out      the output stream
     * @param document the JSON-document
     * @throws IOException IO exception of underlying JSON-writer
     */
    public static void writeTo(OutputStream out, Map<String, Object> document) throws IOException {
        OBJECT_WRITER.writeValue(out, document);
    }

    private DocumentWriter() {
        /* static class */
    }
}
