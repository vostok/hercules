package ru.kontur.vostok.hercules.elastic.sink.index;

import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.util.text.StringUtil;
import ru.kontur.vostok.hercules.util.time.TimeUtil;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

/**
 * Resolves the index name.
 * <p>
 * See Elasticsearch docs for details about index name restrictions:
 * <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-create-index.html">Create index API</a>
 * <p>
 * Inheritors must implement constructors with the same signature as {@link IndexResolver} itself.
 *
 * @author Gregory Koshelev
 */
public abstract class IndexResolver {
    /**
     * Inheritors must implement constructor with the same signature.
     *
     * @param properties properties for the index resolver initialization
     */
    public IndexResolver(Properties properties) {
    }

    private IndexResolver() {
    }

    /**
     * Resolve the index name using the event data.
     *
     * @param event the event
     * @return the optional index name
     */
    public abstract Optional<String> resolve(Event event);

    /**
     * Create resultant index resolver
     * by selection of provided {@link IndexResolver}s and post-processing with considering the index policy
     *
     * @param indexPolicy    the index policy
     * @param indexResolvers list of index resolver with preset order
     * @return index resolver
     */
    public static IndexResolver forPolicy(IndexPolicy indexPolicy, List<IndexResolver> indexResolvers) {
        return new IndexResolver() {
            @Override
            public Optional<String> resolve(Event event) {
                for (IndexResolver indexResolver : indexResolvers) {
                    Optional<String> indexOptional = indexResolver.resolve(event);
                    if (indexOptional.isPresent()) {
                        return indexOptional.
                                map(IndexResolver::sanitize).
                                filter(i -> IndexValidator.isValidIndexName(i) && IndexValidator.isValidLength(i)).
                                map(i -> resolvePostfix(i, event, indexPolicy));
                    }
                }
                return Optional.empty();
            }
        };
    }

    /**
     * Replace illegal characters in the index name with underscore {@code _}
     * and convert all of the characters to lower case.
     * <p>
     * Illegal character is any character which is not alphanumeric, minus sign {@code -}, underscore {@code _} or dot {@code .}.
     *
     * @param index the index name
     * @return the sanitized index name
     */
    private static String sanitize(String index) {
        return StringUtil.sanitize(index, IndexResolver::isCorrectSymbol)
                .toLowerCase();
    }

    private static boolean isCorrectSymbol(int ch) {
        return ch == '-' || ch == '.' || ch == '_'
                || '0' <= ch && ch <= '9'
                || 'A' <= ch && ch <= 'Z'
                || 'a' <= ch && ch <= 'z';
    }

    private static String resolvePostfix(String index, Event event, IndexPolicy indexPolicy) {
        switch (indexPolicy) {
            case STATIC:
            case ILM:
                return index;
            case DAILY:
                return index + "-" + getFormattedDate(event);
            default:
                throw new IllegalArgumentException("Unsupported index policy " + indexPolicy);
        }
    }

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd").withZone(ZoneId.of("UTC"));

    private static String getFormattedDate(Event event) {
        return DATE_FORMATTER.format(TimeUtil.unixTicksToInstant(event.getTimestamp()));
    }
}
