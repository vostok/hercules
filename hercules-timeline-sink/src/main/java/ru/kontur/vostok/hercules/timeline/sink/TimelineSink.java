package ru.kontur.vostok.hercules.timeline.sink;

import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.streams.Consumed;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Predicate;
import ru.kontur.vostok.hercules.cassandra.util.CassandraConnector;
import ru.kontur.vostok.hercules.cassandra.util.Slicer;
import ru.kontur.vostok.hercules.kafka.util.serialization.EventDeserializer;
import ru.kontur.vostok.hercules.kafka.util.serialization.EventSerde;
import ru.kontur.vostok.hercules.kafka.util.serialization.EventSerializer;
import ru.kontur.vostok.hercules.kafka.util.serialization.UuidSerde;
import ru.kontur.vostok.hercules.meta.filter.Filter;
import ru.kontur.vostok.hercules.meta.timeline.Timeline;
import ru.kontur.vostok.hercules.partitioner.HashPartitioner;
import ru.kontur.vostok.hercules.partitioner.NaiveHasher;
import ru.kontur.vostok.hercules.partitioner.RandomPartitioner;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.Variant;

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

    public TimelineSink(Properties properties, Timeline timeline, CassandraConnector cassandraConnector) {
        properties.put(StreamsConfig.APPLICATION_ID_CONFIG, TimelineUtil.timelineToApplicationId(timeline));

        List<String> topics = Arrays.asList(timeline.getStreams());
        Set<String> tags = new HashSet<>(timeline.getFilters().length + timeline.getShardingKey().length);
        final Filter[] filters = timeline.getFilters();
        for (Filter filter : filters) {
            tags.add(filter.getTag());
        }
        tags.addAll(Arrays.asList(timeline.getShardingKey()));

        Predicate<UUID, Event> predicate = (k, v) -> {
            for (Filter filter : filters) {
                Variant value = v.getTag(filter.getTag());
                if (!filter.getCondition().test(value)) {
                    return false;
                }
            }
            return true;
        };

        Slicer slicer =
                new Slicer(
                        new HashPartitioner(new NaiveHasher()),
                        new RandomPartitioner(),
                        timeline.getShardingKey(),
                        timeline.getSlices());

        Serde<UUID> keySerde = new UuidSerde();

        EventSerializer serializer = new EventSerializer();
        EventDeserializer deserializer = EventDeserializer.parseTags(tags);
        Serde<Event> valueSerde = new EventSerde(serializer, deserializer);

        StreamsBuilder builder = new StreamsBuilder();
        KStream<UUID, Event> kStream = builder.stream(topics, Consumed.with(keySerde, valueSerde));
        kStream.filter(predicate).process(() -> new SyncTimelineProcessor(cassandraConnector, timeline, slicer));

        kafkaStreams = new KafkaStreams(builder.build(), properties);
    }

    public void start() {
        kafkaStreams.start();
    }

    public void stop(long timeout, TimeUnit timeUnit) {
        kafkaStreams.close(timeout, timeUnit);
    }
}
