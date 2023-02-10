package ru.kontur.vostok.hercules.graphite.sink;

import ru.kontur.vostok.hercules.application.Application;
import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.sink.AbstractSinkParallelDaemon;
import ru.kontur.vostok.hercules.sink.parallel.sender.ParallelSender;

import java.util.Properties;

public class GraphiteSinkDaemon extends AbstractSinkParallelDaemon<GraphiteSender.GraphitePreparedData> {
    public static void main(String[] args) {
        Application.run(new GraphiteSinkDaemon(), args);
    }

    @Override
    protected ParallelSender<GraphiteSender.GraphitePreparedData> createSender(Properties properties, MetricsCollector metricsCollector) {
        return new GraphiteSender(properties, metricsCollector);
    }

    @Override
    public String getApplicationId() {
        return "sink.graphite";
    }

    @Override
    public String getApplicationName() {
        return "Hercules Graphite Sink";
    }
}
