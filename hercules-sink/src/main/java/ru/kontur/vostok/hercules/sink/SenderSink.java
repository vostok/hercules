package ru.kontur.vostok.hercules.sink;

import ru.kontur.vostok.hercules.kafka.util.consumer.Subscription;
import ru.kontur.vostok.hercules.kafka.util.serialization.EventDeserializer;
import ru.kontur.vostok.hercules.sink.metrics.SinkMetrics;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;

import java.util.Properties;
import java.util.concurrent.ExecutorService;

/**
 * @author Gregory Koshelev
 */
public class SenderSink extends Sink {
    public SenderSink(
            ExecutorService executor,
            String applicationId,
            Properties properties,
            Sender sender,
            SinkMetrics sinkMetrics) {
        super(
                executor,
                applicationId,
                properties,
                sender,
                Subscription.builder().
                        include(PropertiesUtil.get(SinkProps.PATTERN, properties).get()).
                        exclude(PropertiesUtil.get(SinkProps.PATTERN_EXCLUSIONS, properties).get()).
                        build(),
                EventDeserializer.parseAllTags(),
                sinkMetrics);
    }
}
