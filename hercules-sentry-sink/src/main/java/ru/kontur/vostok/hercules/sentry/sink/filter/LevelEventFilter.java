package ru.kontur.vostok.hercules.sentry.sink.filter;

import java.util.Optional;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.util.ContainerUtil;
import ru.kontur.vostok.hercules.sentry.client.SentryLevel;
import ru.kontur.vostok.hercules.sentry.client.SentryLevelParser;
import ru.kontur.vostok.hercules.sink.filter.EventFilter;
import ru.kontur.vostok.hercules.tags.LogEventTags;
import ru.kontur.vostok.hercules.util.parameter.Parameter;
import ru.kontur.vostok.hercules.util.parameter.parsing.ParsingResult;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;

import java.util.Properties;

/**
 * Filters events with level lower required
 *
 * @author Petr Demenev
 */
public class LevelEventFilter extends EventFilter {

    private final SentryLevel level;
    private final SentryLevelParser sentryLevelParser;

    public LevelEventFilter(Properties properties) {
        super(properties);
        this.level = PropertiesUtil.get(Props.LEVEL, properties).get();
        sentryLevelParser = new SentryLevelParser();
    }

    @Override
    public boolean test(Event event) {
        return  ContainerUtil.extract(event.getPayload(), LogEventTags.LEVEL_TAG)
                .map(sentryLevelParser::parse)
                .filter(ParsingResult::hasValue)
                .map(ParsingResult::get)
                .map(value -> level.compareTo(value) >= 0)
                .orElse(false);
    }

    private static class Props {
        static final Parameter<SentryLevel> LEVEL =
                Parameter.enumParameter("level", SentryLevel.class).
                        withDefault(SentryLevel.ERROR).
                        build();
    }
}
