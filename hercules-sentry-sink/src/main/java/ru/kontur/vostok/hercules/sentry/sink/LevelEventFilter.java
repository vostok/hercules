package ru.kontur.vostok.hercules.sentry.sink;

import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.util.ContainerUtil;
import ru.kontur.vostok.hercules.sentry.sink.converters.SentryLevelEnumParser;
import ru.kontur.vostok.hercules.sink.filter.EventFilter;
import ru.kontur.vostok.hercules.tags.LogEventTags;
import ru.kontur.vostok.hercules.util.parameter.Parameter;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;

import java.util.Optional;
import java.util.Properties;

/**
 * Filters events with level lower required
 *
 * @author Petr Demenev
 */
public class LevelEventFilter extends EventFilter {

    private final io.sentry.event.Event.Level level;

    public LevelEventFilter(Properties properties) {
        super(properties);
        this.level = PropertiesUtil.get(Props.LEVEL, properties).get();
    }

    @Override
    public boolean test(Event event) {
        final Optional<io.sentry.event.Event.Level> currentLevel = ContainerUtil.extract(event.getPayload(), LogEventTags.LEVEL_TAG)
                .flatMap(SentryLevelEnumParser::parse);
        return currentLevel.isPresent() && level.compareTo(currentLevel.get()) >= 0;
    }

    private static class Props {
        static final Parameter<io.sentry.event.Event.Level> LEVEL =
                Parameter.enumParameter("level", io.sentry.event.Event.Level.class).
                        withDefault(io.sentry.event.Event.Level.ERROR).
                        build();
    }
}
