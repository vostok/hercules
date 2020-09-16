package ru.kontur.vostok.hercules.elastic.adapter.bulk.action;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

import java.io.IOException;

/**
 * Elastic index action reader.
 *
 * @author Gregory Koshelev
 */
public final class IndexActionReader {
    private static final ObjectReader ACTION_READER;

    static {
        ObjectMapper objectMapper = new ObjectMapper().
                configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        ACTION_READER = objectMapper.readerFor(AnyAction.class);
    }

    /**
     * Read index action.
     *
     * @param data   byte array with UTF-8 encoded JSON-object
     * @param offset JSON-object offset
     * @param length JSON-object length
     * @return index action
     */
    public static IndexAction read(byte[] data, int offset, int length) {
        try {
            AnyAction any = ACTION_READER.readValue(data, offset, length);
            if (AnyAction.isIndexAction(any)) {
                return any.asIndexAction();
            }
        } catch (IOException e) {
            /* do nothing */
        }

        return null;
    }

    private IndexActionReader() {
        /* static class */
    }
}
