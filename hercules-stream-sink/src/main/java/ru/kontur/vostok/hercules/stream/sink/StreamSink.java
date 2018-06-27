package ru.kontur.vostok.hercules.stream.sink;

import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.streams.Consumed;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Predicate;
import org.apache.kafka.streams.kstream.Produced;
import ru.kontur.vostok.hercules.kafka.util.serialization.EventDeserializer;
import ru.kontur.vostok.hercules.kafka.util.serialization.EventSerde;
import ru.kontur.vostok.hercules.kafka.util.serialization.EventSerializer;
import ru.kontur.vostok.hercules.kafka.util.serialization.EventStreamPartitioner;
import ru.kontur.vostok.hercules.kafka.util.serialization.UuidSerde;
import ru.kontur.vostok.hercules.kafka.util.serialization.VoidSerde;
import ru.kontur.vostok.hercules.meta.filter.Filter;
import ru.kontur.vostok.hercules.meta.stream.DerivedStream;
import ru.kontur.vostok.hercules.partitioner.HashPartitioner;
import ru.kontur.vostok.hercules.partitioner.NaiveHasher;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.Variant;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author Gregory Koshelev
 */
public class StreamSink {
    private final KafkaStreams kafkaStreams;

    public StreamSink(Properties properties, DerivedStream derived) {
        properties.put(StreamsConfig.APPLICATION_ID_CONFIG, StreamUtil.streamToApplicationId(derived));
        StreamsConfig config = new StreamsConfig(properties);

        List<String> topics = Arrays.asList(derived.getStreams());
        Set<String> tags = new HashSet<>(derived.getFilters().length + derived.getShardingKey().length);
        final Filter[] filters = derived.getFilters();
        for (Filter filter : filters) {
            tags.add(filter.getTag());
        }
        tags.addAll(Arrays.asList(derived.getShardingKey()));

        Predicate<UUID, Event> predicate = (k, v) -> {
            Map<String, Variant> map = v.getTags();
            for (Filter filter : filters) {
                Variant value = map.get(filter.getTag());
                if (value == null || !filter.getCondition().test(value)) {
                    return false;
                }
            }
            return true;
        };

        Serde<UUID> keySerde = new UuidSerde();

        EventSerializer serializer = new EventSerializer();
        EventDeserializer deserializer = EventDeserializer.parseTags(tags);
        Serde<Event> valueSerde = new EventSerde(serializer, deserializer);

        EventStreamPartitioner partitioner = new EventStreamPartitioner(new HashPartitioner(new NaiveHasher()), derived.getShardingKey(), derived.getPartitions());

        StreamsBuilder builder = new StreamsBuilder();
        KStream<UUID, Event> kStream = builder.stream(topics, Consumed.with(keySerde, valueSerde));
        kStream.filter(predicate).to(derived.getName(), Produced.with(keySerde, valueSerde, partitioner));

        kafkaStreams = new KafkaStreams(builder.build(), config);
    }

    public void start() {
        kafkaStreams.start();
    }

    public void stop(long timeout, TimeUnit timeUnit) {
        kafkaStreams.close(timeout, timeUnit);
    }
}
