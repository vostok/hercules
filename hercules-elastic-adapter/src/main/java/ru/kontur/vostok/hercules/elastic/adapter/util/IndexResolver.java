package ru.kontur.vostok.hercules.elastic.adapter.util;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author Daniil Zhenikhov
 */
public class IndexResolver {
    private final Map<String, List<Pattern>> indexMap;

    public IndexResolver(Map<String, List<String>> indexMap) {
        this.indexMap = transformIndexMap(indexMap);
    }

    /**
     * <code>checkIndex</code> check apiKey and matches index with known patterns.
     *
     * @param apiKey should be checked
     * @param index should be checked
     * @return Status of checking
     */
    public Status checkIndex(String apiKey, String index) {
        if (!indexMap.containsKey(apiKey)) {
            return Status.UNKNOWN;
        }

        for (Pattern pattern: indexMap.get(apiKey)) {
            if (!pattern.matcher(index).matches()) {
                continue;
            }

            return Status.OK;
        }

        return Status.FORBIDDEN;
    }

    /**
     *
     * @param indexMap Map[ApiKey -> List of indexes]
     * @return transform indexes to regex patterns
     */
    private Map<String, List<Pattern>> transformIndexMap(Map<String, List<String>> indexMap) {
        Map<String, List<Pattern>> transformed = new HashMap<>();

        indexMap.forEach((apiKey, indexes) -> {
            transformed.put(apiKey, indexes
                    .stream()
                    .map(this::transformIndexPatternToRegexp)
                    .collect(Collectors.toList()));
        });

        return transformed;
    }

    private Pattern transformIndexPatternToRegexp(String indexPattern) {
        String strPattern = indexPattern
                .replaceAll("\\?", ".")
                .replaceAll("\\*", ".*");

        return Pattern.compile(strPattern);
    }

    public enum Status {
        OK,
        FORBIDDEN,
        UNKNOWN
    }
}
