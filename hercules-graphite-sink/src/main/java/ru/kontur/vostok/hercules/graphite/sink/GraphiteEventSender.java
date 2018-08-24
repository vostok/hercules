package ru.kontur.vostok.hercules.graphite.sink;

import ru.kontur.vostok.hercules.graphite.sink.client.GraphiteClient;
import ru.kontur.vostok.hercules.graphite.sink.client.GraphiteMetricData;
import ru.kontur.vostok.hercules.graphite.sink.client.GraphiteMetricStorage;
import ru.kontur.vostok.hercules.kafka.util.processing.BulkSender;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.fields.MetricsFields;
import ru.kontur.vostok.hercules.protocol.util.ContainerUtil;
import ru.kontur.vostok.hercules.util.time.TimeUtil;

import java.util.Collection;
import java.util.Optional;
import java.util.Properties;

public class GraphiteEventSender implements BulkSender<Event> {

    private final GraphiteClient client;

    public GraphiteEventSender(Properties graphiteProperties) {
        String[] server = graphiteProperties.getProperty("server").split(":", 2);
        this.client = new GraphiteClient(server[0], Integer.valueOf(server[1]));
    }


    @Override
    public void accept(Collection<Event> events) {
        if (events.size() == 0) {
            return;
        }

        GraphiteMetricStorage storage = new GraphiteMetricStorage();

        for (Event event : events) {
            final long timestamp = TimeUtil.gregorianTicksToUnixTime(event.getId().timestamp()) / 1000;
            Optional<String> name = ContainerUtil.extractOptional(event.getPayload(), MetricsFields.METRIC_NAME_FIELD);
            Optional<GraphiteMetricData> value = ContainerUtil.<Double>extractOptional(event.getPayload(), MetricsFields.METRIC_VALUE_FIELD).map(v -> new GraphiteMetricData(timestamp, v));
            if (name.isPresent() && value.isPresent()) {
                storage.add(name.get(), value.get());
            }
        }

        client.send(storage);
    }

    @Override
    public void close() throws Exception {
        /* Do nothing */
    }
}
