package ru.kontur.vostok.hercules.elastic.sink.index;

import org.jetbrains.annotations.Nullable;
import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.Type;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.protocol.hpath.HPath;
import ru.kontur.vostok.hercules.util.parameter.Parameter;
import ru.kontur.vostok.hercules.util.parameter.parsing.ParsingResult;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;
import ru.kontur.vostok.hercules.util.text.StringUtil;
import ru.kontur.vostok.hercules.util.time.TimeUtil;
import ru.kontur.vostok.hercules.util.validation.ValidationResult;

import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Resolves the index name from the event data.
 * <p>
 * See Elasticsearch docs for details about index name restrictions:
 * https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-create-index.html
 *
 * @author Gregory Koshelev
 */
public class IndexResolver {
    private final Function<Event, Optional<String>> function;

    private IndexResolver(Function<Event, Optional<String>> function) {
        this.function = function;
    }

    /**
     * Resolve the index name from the event data.
     *
     * @param event the event
     * @return the optional index name
     */
    public Optional<String> resolve(Event event) {
        return function.apply(event);
    }

    /**
     * Create index resolver for {@link IndexPolicy}.
     *
     * @param indexPolicy the index policy
     * @param properties  the properties
     * @return index resolver
     */
    public static IndexResolver forPolicy(IndexPolicy indexPolicy, Properties properties) {
        if (indexPolicy == IndexPolicy.STATIC) {
            Parameter<String>.ParameterValue indexNameValue = PropertiesUtil.get(Props.INDEX_NAME, properties);
            if (indexNameValue.isEmpty()) {
                throw new IllegalArgumentException("Index name must be defined if 'static' index policy is used");
            }
            Optional<String> indexNameOptional = Optional.of(indexNameValue.get()).map(IndexResolver::sanitize);
            return new IndexResolver(e -> indexNameOptional);

        }

        //TODO: Should be replaced with multiple indexTags alternatives
        Parameter<HPath>.ParameterValue indexPath = PropertiesUtil.get(Props.INDEX_PATH, properties);
        String[] indexTags = PropertiesUtil.get(Props.INDEX_TAGS, properties).get();
        List<IndexPart> indexParts = new ArrayList<>(indexTags.length);
        for (String indexTag : indexTags) {
            boolean isOptional = indexTag.endsWith("?");
            HPath path = HPath.fromPath(isOptional ? indexTag.substring(0, indexTag.length() - 1) : indexTag);

            indexParts.add(new IndexPart(path, isOptional));
        }
        PrefixResolver prefixResolver = new PrefixResolver(indexPath.orEmpty(null), indexParts);

        switch (indexPolicy) {
            case ILM:
                return new IndexResolver(prefixResolver::resolve);
            case DAILY:
                return new IndexResolver(e -> {
                    Optional<String> prefix = prefixResolver.resolve(e);
                    return prefix.map(p -> p + "-" + getFormattedDate(e));
                });
            default:
                throw new IllegalArgumentException("Unsupported index policy " + indexPolicy);
        }
    }

    private static final Pattern ILLEGAL_CHARS = Pattern.compile("[^-a-zA-Z0-9_.]");

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
        return ILLEGAL_CHARS.matcher(index).replaceAll("_").
                toLowerCase();
    }

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd").withZone(ZoneId.of("UTC"));

    private static String getFormattedDate(Event event) {
        return DATE_FORMATTER.format(TimeUtil.unixTicksToInstant(event.getTimestamp()));
    }

    /**
     * Resolves the index prefix from the event data using following algorithm:<br>
     * 1. If full index path defined then try to use this tag.<br>
     * 2. Try to build index prefix from configured parts (optional and required tags).
     * <p>
     * Parts are joined with dash {@code '-'}. The index prefix should satisfy Elastic restrictions.
     */
    private static class PrefixResolver {
        private final HPath indexPath;
        private final List<IndexPart> indexParts;

        PrefixResolver(@Nullable HPath indexPath, List<IndexPart> indexParts) {
            this.indexPath = indexPath;
            this.indexParts = indexParts;
        }

        Optional<String> resolve(Event event) {
            Container payload = event.getPayload();

            if (indexPath != null) {
                Variant variant = indexPath.extract(payload);
                if (variant != null && variant.getType() == Type.STRING) {
                    String index = new String((byte[]) variant.getValue(), StandardCharsets.UTF_8);
                    if (!IndexValidator.isValidIndexName(index) || !IndexValidator.isValidLength(index)) {
                        return Optional.empty();
                    }
                    return Optional.of(IndexResolver.sanitize(index));
                }
            }

            List<String> parts = new ArrayList<>(indexParts.size());
            for (IndexPart indexPart : indexParts) {
                Variant variant = indexPart.path.extract(payload);
                if ((variant == null || variant.getType() != Type.STRING)) {
                    if (indexPart.optional) {
                        continue;
                    }
                    return Optional.empty();
                }

                parts.add(IndexResolver.sanitize(new String((byte[]) variant.getValue(), StandardCharsets.UTF_8)));
            }
            String prefix = String.join("-", parts);
            if (!IndexValidator.isValidIndexName(prefix) || !IndexValidator.isValidLength(prefix)) {
                return Optional.empty();
            }
            return Optional.of(prefix);
        }
    }

    private static class IndexPart {
        private final HPath path;
        private final boolean optional;

        IndexPart(HPath path, boolean optional) {
            this.path = path;
            this.optional = optional;
        }
    }

    static class Props {
        static final Parameter<String> INDEX_NAME =
                Parameter.stringParameter("index.name").
                        withValidator((v) -> {
                            if (!IndexValidator.isValidIndexName(v)) {
                                return ValidationResult.error("Invalid index name");
                            }
                            if (!IndexValidator.isValidLength(v)) {
                                return ValidationResult.error("Index name exceeds length limit");
                            }
                            return ValidationResult.ok();
                        }).
                        build();

        static final Parameter<HPath> INDEX_PATH =
                Parameter.parameter("index.path", v -> {
                    if (StringUtil.isNullOrEmpty(v)) {
                        return ParsingResult.empty();
                    }
                    return ParsingResult.of(HPath.fromPath(v));
                }).
                        build();

        static final Parameter<String[]> INDEX_TAGS =
                Parameter.stringArrayParameter("index.tags").
                        withDefault(new String[0]).
                        build();
    }
}
