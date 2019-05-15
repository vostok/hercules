package ru.kontur.vostok.hercules.timeline.sink;

import com.codahale.metrics.Meter;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Predicate;
import ru.kontur.vostok.hercules.cassandra.util.CassandraConnector;
import ru.kontur.vostok.hercules.cassandra.util.Slicer;
import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.kafka.util.serialization.EventDeserializer;
import ru.kontur.vostok.hercules.kafka.util.serialization.EventSerde;
import ru.kontur.vostok.hercules.kafka.util.serialization.EventSerializer;
import ru.kontur.vostok.hercules.kafka.util.serialization.UuidSerde;
import ru.kontur.vostok.hercules.meta.filter.Filter;
import ru.kontur.vostok.hercules.meta.timeline.Timeline;
import ru.kontur.vostok.hercules.partitioner.HashPartitioner;
import ru.kontur.vostok.hercules.partitioner.NaiveHasher;
import ru.kontur.vostok.hercules.partitioner.RandomPartitioner;
import ru.kontur.vostok.hercules.partitioner.ShardingKey;
import ru.kontur.vostok.hercules.protocol.Event;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author Gregory Koshelev
 */
public class TimelineSink {
    private final KafkaStreams kafkaStreams;

    public TimelineSink(
            Properties properties,
            Timeline timeline,
            CassandraConnector cassandraConnector,
            MetricsCollector metricsCollector
    ) {
        properties.put(StreamsConfig.APPLICATION_ID_CONFIG, TimelineUtil.timelineToApplicationId(timeline));

        List<String> topics = Arrays.asList(timeline.getStreams());
        final Filter[] filters = timeline.getFilters();

        Set<String> tags = new HashSet<>(filters.length + timeline.getShardingKey().length);
        for (Filter filter : filters) {
            tags.add(filter.getHPath().getRootTag());//TODO: Should be revised (do not parse all the tag tree if the only tag chain is needed)
        }
        tags.addAll(Arrays.asList(timeline.getShardingKey()));

        Meter receivedEventCountMeter = metricsCollector.meter("receivedEventCount");
        Meter filteredEventCountMeter = metricsCollector.meter("filteredEventCount");

        Predicate<UUID, Event> predicate = (k, event) -> {
            for (Filter filter : filters) {
                if (!filter.test(event.getPayload())) {
                    filteredEventCountMeter.mark();
                    return false;
                }
            }
            return true;
        };

        Slicer slicer =
                new Slicer(
                        new HashPartitioner(new NaiveHasher()),
                        new RandomPartitioner(),
                        ShardingKey.fromKeyPaths(timeline.getShardingKey()),
                        timeline.getSlices());

        Serde<UUID> keySerde = new UuidSerde();

        EventSerializer serializer = new EventSerializer();
        EventDeserializer deserializer = EventDeserializer.parseTags(tags);
        Serde<Event> valueSerde = new EventSerde(serializer, deserializer);

        StreamsBuilder builder = new StreamsBuilder();
        KStream<UUID, Event> kStream = builder.stream(topics, Consumed.with(keySerde, valueSerde));
        kStream.peek((k, v) -> receivedEventCountMeter.mark())
                .filter(predicate)
                .process(() -> new SyncTimelineProcessor(cassandraConnector, timeline, slicer, metricsCollector));

        kafkaStreams = new KafkaStreams(builder.build(), properties);
    }

    public void start() {
        kafkaStreams.start();
    }

    public void stop(long timeout, TimeUnit timeUnit) {
        kafkaStreams.close(timeout, timeUnit);
    }
}
