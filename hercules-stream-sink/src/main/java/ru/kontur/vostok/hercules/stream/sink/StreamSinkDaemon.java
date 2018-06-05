package ru.kontur.vostok.hercules.stream.sink;

import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Predicate;
import org.apache.kafka.streams.kstream.Produced;
import ru.kontur.vostok.hercules.kafka.util.EventDeserializer;
import ru.kontur.vostok.hercules.kafka.util.EventSerde;
import ru.kontur.vostok.hercules.kafka.util.EventSerializer;
import ru.kontur.vostok.hercules.kafka.util.EventStreamPartitioner;
import ru.kontur.vostok.hercules.kafka.util.VoidSerde;
import ru.kontur.vostok.hercules.partitioner.HashPartitioner;
import ru.kontur.vostok.hercules.partitioner.NaiveHasher;
import ru.kontur.vostok.hercules.protocol.Event;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * @author Gregory Koshelev
 */
public class StreamSinkDaemon {
    private static final Object lock = new Object();
    private static KafkaStreams kafkaStreams;

    public static void main(String[] args) {
        Properties props = new Properties();
        StreamsConfig config = new StreamsConfig(props);
        //TODO: Deserializer!

        String derived = "stream.derived";
        List<String> topics = Collections.singletonList("stream-test");
        Set<String> tags = new HashSet<>(Arrays.asList("host", "timestamp"));
        String[] shardingKey = {"host"};
        int partitions = 2;

        Predicate<Void, Event> filter = (k, v) -> {
            return true;
        };

        Serde<Void> keySerde = new VoidSerde();

        EventSerializer serializer = new EventSerializer();
        EventDeserializer deserializer = new EventDeserializer(tags);
        Serde<Event> valueSerde = new EventSerde(serializer, deserializer);

        EventStreamPartitioner partitioner = new EventStreamPartitioner(new HashPartitioner(new NaiveHasher()), shardingKey, partitions);

        StreamsBuilder builder = new StreamsBuilder();
        KStream<Void, Event> kStream = builder.stream(topics);
        kStream.filter(filter).to(derived, Produced.with(keySerde, valueSerde, partitioner));

        kafkaStreams = new KafkaStreams(builder.build(), config);

        Runtime.getRuntime().addShutdownHook(new Thread(StreamSinkDaemon::shutdown));

        kafkaStreams.start();
    }

    private static void shutdown() {
        synchronized (lock) {
            if (kafkaStreams != null) {
                kafkaStreams.close(5_000, TimeUnit.MILLISECONDS);
            }
        }
    }
}
