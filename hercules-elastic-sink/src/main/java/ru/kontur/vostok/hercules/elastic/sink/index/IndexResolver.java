package ru.kontur.vostok.hercules.elastic.sink.index;

import ru.kontur.vostok.hercules.protocol.Event;

import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Resolves index name from event data.
 * <p>
 * See Elasticsearch docs for details about index name restrictions:
 * https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-create-index.html
 *
 * @author Gregory Koshelev
 */
public interface IndexResolver {
    Optional<String> resolve(Event event);

    Pattern ILLEGAL_CHARS = Pattern.compile("[^-a-zA-Z0-9_.]");

    /**
     * Replace illegal characters in the index name with underscore {@code _}
     * and convert all of the characters to lower case.
     * <p>
     * Illegal character is any character which is not alphanumeric, minus sign {@code -}, underscore {@code _} or dot {@code .}.
     *
     * @param index the index name
     * @return the sanitized index name
     */
    static String sanitize(String index) {
        return ILLEGAL_CHARS.matcher(index).replaceAll("_").
                toLowerCase();
    }
}
