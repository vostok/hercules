package ru.kontur.vostok.hercules.graphite.sink;

import ru.kontur.vostok.hercules.graphite.sink.client.GraphiteClient;
import ru.kontur.vostok.hercules.graphite.sink.client.GraphiteMetric;
import ru.kontur.vostok.hercules.graphite.sink.client.GraphiteMetricStorage;
import ru.kontur.vostok.hercules.kafka.util.processing.BulkSender;
import ru.kontur.vostok.hercules.kafka.util.processing.Entry;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.util.time.TimeUtil;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;

public class GraphiteEventSender implements BulkSender<UUID, Event> {

    private final GraphiteClient client;

    public GraphiteEventSender(Properties graphiteProperties) {
        String[] server = graphiteProperties.getProperty("server").split(":", 2);
        this.client = new GraphiteClient(server[0], Integer.valueOf(server[1]));
    }

    @Override
    public void send(Collection<Entry<UUID, Event>> events) {
        if (events.size() == 0) {
            return;
        }

        GraphiteMetricStorage storage = new GraphiteMetricStorage();

        for (Entry<UUID, Event> entry : events) {
            Optional<String> name = extractMetricName(entry.getValue());
            Optional<GraphiteMetric> value = extractMetricValue(entry.getValue());
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

    private static Optional<String> extractMetricName(Event event) {
        return Optional.ofNullable(event.getTags().get("metric-name"))
                .map(Variant::getValue)
                .map(o -> new String((byte[]) o, StandardCharsets.UTF_8));
    }

    private static Optional<GraphiteMetric> extractMetricValue(Event event) {
        final long timestamp = TimeUtil.gregorianTicksToUnixTime(event.getId().timestamp()) / 1000;

        return Optional.ofNullable(event.getTags().get("metric-value"))
                .map(Variant::getValue)
                .map(o -> new GraphiteMetric(timestamp, (Double) o));
    }
}
