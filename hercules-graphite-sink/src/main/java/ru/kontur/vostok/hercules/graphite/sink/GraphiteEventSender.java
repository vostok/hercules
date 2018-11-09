package ru.kontur.vostok.hercules.graphite.sink;

import com.codahale.metrics.Timer;
import ru.kontur.vostok.hercules.graphite.client.DefaultGraphiteClientRetryStrategy;
import ru.kontur.vostok.hercules.graphite.client.GraphiteClient;
import ru.kontur.vostok.hercules.graphite.client.GraphiteMetricData;
import ru.kontur.vostok.hercules.graphite.client.GraphiteMetricDataSender;
import ru.kontur.vostok.hercules.kafka.util.processing.BackendServiceFailedException;
import ru.kontur.vostok.hercules.kafka.util.processing.bulk.BulkSender;
import ru.kontur.vostok.hercules.kafka.util.processing.bulk.BulkSenderStat;
import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.tags.MetricsTags;
import ru.kontur.vostok.hercules.protocol.util.ContainerUtil;
import ru.kontur.vostok.hercules.util.properties.PropertyDescription;
import ru.kontur.vostok.hercules.util.properties.PropertyDescriptions;
import ru.kontur.vostok.hercules.util.time.TimeUtil;
import ru.kontur.vostok.hercules.util.validation.Validators;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class GraphiteEventSender implements BulkSender<Event> {

    private static class Props {
        static final PropertyDescription<String> GRAPHITE_HOST = PropertyDescriptions
                .stringProperty("server.host")
                .build();

        static final PropertyDescription<Integer> GRAPHITE_PORT = PropertyDescriptions
                .integerProperty("server.port")
                .withValidator(Validators.portValidator())
                .build();
    }

    private final GraphiteMetricDataSender sender;
    private final Timer graphiteClientTimer;

    public GraphiteEventSender(
            Properties graphiteProperties,
            MetricsCollector metricsCollector
    ) {
        final String graphiteHost = Props.GRAPHITE_HOST.extract(graphiteProperties);
        final int graphitePort = Props.GRAPHITE_PORT.extract(graphiteProperties);

        this.sender = new DefaultGraphiteClientRetryStrategy(new GraphiteClient(graphiteHost, graphitePort));

        graphiteClientTimer = metricsCollector.timer("graphiteClientRequestTimeMs");
    }

    @Override
    public BulkSenderStat process(Collection<Event> events) throws BackendServiceFailedException {
        if (events.size() == 0) {
            return BulkSenderStat.ZERO;
        }

        List<GraphiteMetricData> data = new ArrayList<>(events.size());

        int processed = 0;
        int dropped = 0;
        for (Event event : events) {
            final long timestamp = TimeUtil.gregorianTicksToUnixTime(event.getId().timestamp()) / 1000;
            Optional<String> name = ContainerUtil.extract(event.getPayload(), MetricsTags.METRIC_NAME_TAG);
            Optional<Double> value = ContainerUtil.extract(event.getPayload(), MetricsTags.METRIC_VALUE_TAG);
            if (name.isPresent() && value.isPresent()) {
                data.add(new GraphiteMetricData(name.get(), timestamp, value.get()));
                processed++;
            } else {
                dropped++;
            }
        }

        try {
            long start = System.currentTimeMillis();
            sender.send(data);
            graphiteClientTimer.update(System.currentTimeMillis() - start, TimeUnit.MILLISECONDS);
        } catch (IOException e) {
            throw new BackendServiceFailedException(e);
        }
        return new BulkSenderStat(processed, dropped);
    }

    @Override
    public void close() throws Exception {
        /* Do nothing */
    }
}
