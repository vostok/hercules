package ru.kontur.vostok.hercules.graphite.sink;

import com.codahale.metrics.Timer;
import ru.kontur.vostok.hercules.graphite.sink.client.GraphiteClient;
import ru.kontur.vostok.hercules.graphite.sink.client.GraphiteMetricData;
import ru.kontur.vostok.hercules.graphite.sink.client.GraphiteMetricStorage;
import ru.kontur.vostok.hercules.kafka.util.processing.BulkSender;
import ru.kontur.vostok.hercules.kafka.util.processing.BulkSenderStat;
import ru.kontur.vostok.hercules.metrics.MetricsCollector;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.tags.MetricsTags;
import ru.kontur.vostok.hercules.protocol.util.ContainerUtil;
import ru.kontur.vostok.hercules.util.time.TimeUtil;

import java.util.Collection;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class GraphiteEventSender implements BulkSender<Event> {

    private final GraphiteClient client;
    private final Timer graphiteClientTimer;

    public GraphiteEventSender(
            Properties graphiteProperties,
            MetricsCollector metricsCollector
    ) {
        String[] server = graphiteProperties.getProperty("server").split(":", 2);
        this.client = new GraphiteClient(server[0], Integer.valueOf(server[1]));

        graphiteClientTimer = metricsCollector.timer("graphiteClient");
    }

    @Override
    public BulkSenderStat process(Collection<Event> events) {
        if (events.size() == 0) {
            return BulkSenderStat.ZERO;
        }

        GraphiteMetricStorage storage = new GraphiteMetricStorage();

        int processed = 0;
        int dropped = 0;
        for (Event event : events) {
            final long timestamp = TimeUtil.gregorianTicksToUnixTime(event.getId().timestamp()) / 1000;
            Optional<String> name = ContainerUtil.extract(event.getPayload(), MetricsTags.METRIC_NAME_TAG);
            Optional<GraphiteMetricData> value = ContainerUtil.extract(event.getPayload(), MetricsTags.METRIC_VALUE_TAG).map(v -> new GraphiteMetricData(timestamp, v));
            if (name.isPresent() && value.isPresent()) {
                storage.add(name.get(), value.get());
                processed++;
            } else {
                dropped++;
            }
        }

        graphiteClientTimer.time(() -> client.send(storage));
        return new BulkSenderStat(processed, dropped);
    }

    @Override
    public void close() throws Exception {
        /* Do nothing */
    }
}
