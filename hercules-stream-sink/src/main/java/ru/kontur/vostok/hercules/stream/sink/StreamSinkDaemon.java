package ru.kontur.vostok.hercules.sink.stream;

import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Predicate;
import org.apache.kafka.streams.kstream.Produced;
import org.omg.CORBA.ServerRequest;
import ru.kontur.vostok.hercules.protocol.Event;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
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
        Predicate<Void, Event> filter = (k, v) -> {
            return true;
        };

        Serde<Void> keySerde = new Serde<Void>() {
            @Override
            public void configure(Map<String, ?> configs, boolean isKey) {

            }

            @Override
            public void close() {

            }

            @Override
            public Serializer<Void> serializer() {
                return ;
            }

            @Override
            public Deserializer<Void> deserializer() {
                return null;
            }
        };

        Serde<Event> valueSerde = new Serde<Event>() {
            @Override
            public void configure(Map<String, ?> configs, boolean isKey) {

            }

            @Override
            public void close() {

            }

            @Override
            public Serializer<Event> serializer() {
                return null;
            }

            @Override
            public Deserializer<Event> deserializer() {
                return null;
            }
        }

        StreamsBuilder builder = new StreamsBuilder();
        KStream<Void, Event> kStream = builder.stream(topics);
        kStream.filter(filter).to(derived, Produced.with());

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
