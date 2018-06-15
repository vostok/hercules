package ru.kontur.vostok.hercules.elasticsearch.sink;

import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.streams.Consumed;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import ru.kontur.vostok.hercules.kafka.util.processing.BulkProcessor;
import ru.kontur.vostok.hercules.kafka.util.serialization.EventDeserializer;
import ru.kontur.vostok.hercules.kafka.util.serialization.EventSerde;
import ru.kontur.vostok.hercules.kafka.util.serialization.EventSerializer;
import ru.kontur.vostok.hercules.kafka.util.serialization.VoidSerde;
import ru.kontur.vostok.hercules.protocol.Event;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class ElasticSearchSink {

    private final KafkaStreams kafkaStreams;

    public ElasticSearchSink(Properties streamsProperties, Properties elasticsearchProperties) {
        ElasticSearchEventSender eventSender = new ElasticSearchEventSender(elasticsearchProperties);

        Serde<Void> keySerde = new VoidSerde();

        EventSerializer serializer = new EventSerializer();
        EventDeserializer deserializer = new EventDeserializer();
        Serde<Event> valueSerde = new EventSerde(serializer, deserializer);

        StreamsBuilder streamsBuilder = new StreamsBuilder();
        streamsBuilder.<Void, Event>stream("test-elastic-sink", Consumed.with(keySerde, valueSerde))
                .process(() -> new BulkProcessor<>(eventSender::send, 100_000, 1000));

        this.kafkaStreams = new KafkaStreams(streamsBuilder.build(), streamsProperties);
    }

    public void start() {
        kafkaStreams.start();
    }

    public void stop(int timeout, TimeUnit timeUnit) {
        kafkaStreams.close(timeout, timeUnit);
    }
}
