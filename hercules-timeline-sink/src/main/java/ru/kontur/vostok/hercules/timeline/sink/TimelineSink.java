package ru.kontur.vostok.hercules.timeline.sink;

import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.kafka.util.serialization.EventDeserializer;
import ru.kontur.vostok.hercules.meta.filter.Filter;
import ru.kontur.vostok.hercules.meta.timeline.Timeline;
import ru.kontur.vostok.hercules.partitioner.ShardingKey;
import ru.kontur.vostok.hercules.protocol.TinyString;
import ru.kontur.vostok.hercules.protocol.hpath.HPath;
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

        Set<TinyString> tags = new HashSet<>(filters.length + timeline.getShardingKey().length);
        for (Filter filter : filters) {
            tags.add(filter.getHPath().getRootTag());//TODO: Should be revised (do not parse all the tag tree if the only tag chain is needed)
        }
        Arrays.stream(ShardingKey.fromKeyPaths(timeline.getShardingKey()).getKeys()).
                map(HPath::getRootTag).
                forEach(tags::add);//TODO: Should be revised (do not parse all the tag tree if the only tag chain is needed)

        return EventDeserializer.parseTags(tags);
    }
}
