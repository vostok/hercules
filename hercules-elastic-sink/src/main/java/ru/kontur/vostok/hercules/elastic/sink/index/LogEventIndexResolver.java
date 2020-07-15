package ru.kontur.vostok.hercules.elastic.sink.index;

import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.util.ContainerUtil;
import ru.kontur.vostok.hercules.tags.CommonTags;
import ru.kontur.vostok.hercules.tags.ElasticSearchTags;
import ru.kontur.vostok.hercules.util.time.TimeUtil;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Resolves index name from event data.
 * <p>
 * See Elasticsearch docs for details about index name restrictions:
 * https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-create-index.html
 *
 * @author Gregory Koshelev
 */
public class LogEventIndexResolver implements IndexResolver {
    private final Function<Event, Optional<String>> function;

    private LogEventIndexResolver(Function<Event, Optional<String>> function) {
        this.function = function;
    }

    @Override
    public Optional<String> resolve(Event event) {
        return function.apply(event);
    }

    public static LogEventIndexResolver forPolicy(IndexPolicy policy) {
        switch (policy) {
            case DAILY:
                return new LogEventIndexResolver(LogEventIndexResolver::resolveDailyIndex);
            case ILM:
                return new LogEventIndexResolver(LogEventIndexResolver::resolveIlmIndex);
            default:
                throw new IllegalArgumentException("Unsupported index policy " + policy);
        }
    }

    private static Optional<String> resolveDailyIndex(Event event) {
        Optional<String> indexPrefix = getIndexPrefix(event);
        return indexPrefix.flatMap(prefix -> Optional.of(prefix + "-" + getFormattedDate(event)));
    }

    private static Optional<String> resolveIlmIndex(Event event) {
        return getIndexPrefix(event);
    }

    private static Optional<String> getIndexPrefix(Event event) {
        Optional<Container> properties = ContainerUtil.extract(event.getPayload(), CommonTags.PROPERTIES_TAG);
        if (!properties.isPresent()) {
            return Optional.empty();
        }

        Optional<String> index = ContainerUtil.extract(properties.get(), ElasticSearchTags.ELK_INDEX_TAG);
        if (index.isPresent()) {
            if (!IndexValidator.isValidIndexName(index.get()) || !IndexValidator.isValidLength(index.get())) {
                return Optional.empty();
            }
            return index.map(IndexResolver::sanitize);
        }

        Optional<String> project = ContainerUtil.extract(properties.get(), CommonTags.PROJECT_TAG);
        if (!project.isPresent() || !IndexValidator.isValidIndexName(project.get())) {
            return Optional.empty();
        }

        Optional<String> environment = ContainerUtil.extract(properties.get(), CommonTags.ENVIRONMENT_TAG);
        Optional<String> subproject = ContainerUtil.extract(properties.get(), CommonTags.SUBPROJECT_TAG);

        String prefix = Stream.of(project, environment, subproject).
                filter(Optional::isPresent).
                map(Optional::get).
                map(IndexResolver::sanitize).
                collect(Collectors.joining("-"));

        if (!IndexValidator.isValidLength(prefix)) {
            return Optional.empty();
        }

        return Optional.of(prefix);
    }

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd").withZone(ZoneId.of("UTC"));

    private static String getFormattedDate(Event event) {
        return DATE_FORMATTER.format(TimeUtil.unixTicksToInstant(event.getTimestamp()));
    }
}
