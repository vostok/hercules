package ru.kontur.vostok.hercules.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

import java.io.IOException;
import java.util.Map;

/**
 * Read document from JSON-object.
 * Document is structured model consists of Maps, Lists, Strings and primitives.
 * <p>
 * Sample:
 * <pre>
 * {
 *   "map": {"field1": "value1", "field2": "value2"}, // parsed as Map with String keys and String values
 *   "listOfStrings": ["a", "b", "c"],                // parsed as List with String elements
 *   "listOfIntegers": [1, 2, 3]                      // parsed as List with Integer elements
 * }
 * </pre>
 *
 * @author Gregory Koshelev
 */
public final class DocumentReader {
    private static final ObjectReader DOCUMENT_READER;

    static {
        ObjectMapper objectMapper = new ObjectMapper();

        DOCUMENT_READER = objectMapper.readerFor(Map.class);
    }

    /**
     * Parse JSON-object to document which is structured model consists of Maps, Lists, Strings and primitives.
     *
     * @param data byte array with UTF-8 encoded JSON-object
     * @return document
     */
    public static Document read(byte[] data) {
        return read(data, 0, data.length);
    }

    /**
     * Parse JSON-object to document which is structured model consists of Maps, Lists, Strings and primitives.
     *
     * @param data   byte array with UTF-8 encoded JSON-object
     * @param offset JSON-object offset
     * @return document
     */
    public static Document read(byte[] data, int offset) {
        return read(data, offset, data.length - offset);
    }

    /**
     * Parse JSON-object to document which is structured model consists of Maps, Lists, Strings and primitives.
     * <p>
     * Primarily use case is to parse JSON-object from {@code application/x-ndjson} data in UTF-8 encoding.
     *
     * @param data   byte array with UTF-8 encoded JSON-object
     * @param offset JSON-object offset
     * @param length JSON-object length
     * @return document
     */
    public static Document read(byte[] data, int offset, int length) {
        try {
            return Document.of(DOCUMENT_READER.readValue(data, offset, length));
        } catch (IOException e) {
            //TODO: May be process exception?
            return null;
        }
    }

    private DocumentReader() {
        /* static class */
    }
}
