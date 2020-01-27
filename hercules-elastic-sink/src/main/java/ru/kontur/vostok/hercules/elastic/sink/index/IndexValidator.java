package ru.kontur.vostok.hercules.elastic.sink.index;

import ru.kontur.vostok.hercules.util.text.CharUtil;

/**
 * @author Vladimir Tsypaev
 */
public class IndexValidator {
    private static final int MAX_INDEX_SIZE_BYTES = 120;

    /**
     * Check index name not empty and start with not forbidden character.
     */
    public static boolean isValidIndexName(String index) {
        if (index.isEmpty()) {
            return false;
        }
        return CharUtil.isAlphaNumeric(index.charAt(0));
    }

    /**
     * Check index name length equals or less then {@code MAX_INDEX_SIZE_BYTES}.
     */
    public static boolean isValidLength(String index) {
        return index.getBytes().length <= MAX_INDEX_SIZE_BYTES;
    }

    private IndexValidator() {
        /* static class */
    }
}
