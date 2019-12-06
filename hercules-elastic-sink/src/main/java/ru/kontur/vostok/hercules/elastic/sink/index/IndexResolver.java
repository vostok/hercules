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
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Gregory Koshelev
 */
public class IndexResolver {
    private final Function<Event, Optional<String>> function;

    private IndexResolver(Function<Event, Optional<String>> function) {
        this.function = function;
    }

    public Optional<String> resolve(Event event) {
        return function.apply(event);
    }

    public static IndexResolver forPolicy(IndexPolicy policy) {
        switch (policy) {
            case DAILY:
                return new IndexResolver(IndexResolver::resolveDailyIndex);
            case ILM:
                return new IndexResolver(IndexResolver::resolveIlmIndex);
            default:
                throw new IllegalArgumentException("Unknown index policy " + policy);
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
            return index;
        }

        Optional<String> project = ContainerUtil.extract(properties.get(), CommonTags.PROJECT_TAG);
        if (!project.isPresent()) {
            return Optional.empty();
        }

        Optional<String> environment = ContainerUtil.extract(properties.get(), CommonTags.ENVIRONMENT_TAG);
        Optional<String> subproject = ContainerUtil.extract(properties.get(), CommonTags.SUBPROJECT_TAG);

        String prefix = Stream.of(project, environment, subproject).
                filter(Optional::isPresent).
                map(Optional::get).
                map(String::toLowerCase).
                map(part -> part.replace(' ', '_')).
                collect(Collectors.joining("-"));
        if (!validate(prefix)) {
            return Optional.empty();
        }
        return Optional.of(prefix);
    }

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd").withZone(ZoneId.of("UTC"));
    private static String getFormattedDate(Event event) {
        return DATE_FORMATTER.format(TimeUtil.unixTicksToInstant(event.getTimestamp()));
    }

    private static boolean validate(String prefix) {
        for (int i = 0; i < prefix.length(); i++) {
            char c = prefix.charAt(i);
            if (!isLowerCaseLatin(c) && !isDigit(c) && !isUnderscore(c) && !isDot(c) && !isMinusSign(c)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isLowerCaseLatin(char c) {
        return c >= 'a' && c <= 'z';
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private static boolean isUnderscore(char c) {
        return c == '_';
    }

    private static boolean isDot(char c) {
        return c == '.';
    }

    private static boolean isMinusSign(char c) {
        return c == '-';
    }
}
