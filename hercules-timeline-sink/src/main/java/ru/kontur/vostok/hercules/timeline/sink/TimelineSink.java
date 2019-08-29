package ru.kontur.vostok.hercules.timeline.sink;

import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.kafka.util.serialization.EventDeserializer;
import ru.kontur.vostok.hercules.meta.filter.Filter;
import ru.kontur.vostok.hercules.meta.timeline.Timeline;
import ru.kontur.vostok.hercules.sink.Sink;
import ru.kontur.vostok.hercules.util.PatternMatcher;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Gregory Koshelev
 */
public class TimelineSink extends Sink {
    public TimelineSink(
            ExecutorService executor,
            Properties properties,
            TimelineSender sender,
            MetricsCollector metricsCollector,
            Timeline timeline) {
        super(
                executor,
                TimelineUtil.timelineToApplicationId(timeline),
                properties,
                sender,
                Stream.of(timeline.getStreams()).map(PatternMatcher::new).collect(Collectors.toList()),
                fromTimeline(timeline),
                metricsCollector);
    }

    private static EventDeserializer fromTimeline(Timeline timeline) {
        final Filter[] filters = timeline.getFilters();

        Set<String> tags = new HashSet<>(filters.length + timeline.getShardingKey().length);
        for (Filter filter : filters) {
            tags.add(filter.getHPath().getRootTag());//TODO: Should be revised (do not parse all the tag tree if the only tag chain is needed)
        }
        tags.addAll(Arrays.asList(timeline.getShardingKey()));

        return EventDeserializer.parseTags(tags);
    }
}
