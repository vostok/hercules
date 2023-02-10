package ru.kontur.vostok.hercules.elastic.sink;

import ru.kontur.vostok.hercules.application.Application;
import ru.kontur.vostok.hercules.configuration.Scopes;
import ru.kontur.vostok.hercules.elastic.sink.index.IndexPolicy;
import ru.kontur.vostok.hercules.elastic.sink.metrics.TopicWithIndexMetricsCollector;
import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.protocol.util.EventUtil;
import ru.kontur.vostok.hercules.sink.AbstractSinkParallelDaemon;
import ru.kontur.vostok.hercules.sink.parallel.EventsBatch;
import ru.kontur.vostok.hercules.sink.parallel.EventsBatchListener;
import ru.kontur.vostok.hercules.sink.parallel.sender.ParallelSender;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;

import java.util.Map;
import java.util.Properties;

/**
 * @author Gregory Koshelev
 */
public class ElasticSinkDaemon extends AbstractSinkParallelDaemon<ElasticSender.ElasticPreparedData> {
    public static void main(String[] args) {
        Application.run(new ElasticSinkDaemon(), args);
    }

    @Override
    protected ParallelSender<ElasticSender.ElasticPreparedData> createSender(Properties elasticProperties, MetricsCollector metricsCollector) {
        IndexPolicy indexPolicy = PropertiesUtil.get(ElasticSender.Props.INDEX_POLICY, elasticProperties).get();

        ElasticClientImpl elasticClient = new ElasticClientImpl(
                PropertiesUtil.ofScope(elasticProperties, "elastic.client"), indexPolicy, metricsCollector
        );

        boolean leproseryEnable = PropertiesUtil.get(ElasticSender.Props.LEPROSERY_ENABLE, elasticProperties).get();
        LeproserySender leproserySender = leproseryEnable
                ? new LeproserySender(PropertiesUtil.ofScope(elasticProperties, Scopes.LEPROSERY), metricsCollector)
                : null;

        return new ElasticSender(elasticProperties, indexPolicy, elasticClient, leproserySender, metricsCollector);
    }

    @Override
    protected EventsBatchListener<ElasticSender.ElasticPreparedData> eventsBatchListener(MetricsCollector metricsCollector) {
        return new ElasticEventsBatchListener(metricsCollector);
    }

    /**
     * Collect metrics by detailed information about events
     *
     * @author Innokentiy Krivonosov
     */
    static class ElasticEventsBatchListener implements EventsBatchListener<ElasticSender.ElasticPreparedData> {
        private final TopicWithIndexMetricsCollector topicWithIndexMetricsCollector;

        public ElasticEventsBatchListener(MetricsCollector metricsCollector) {
            this.topicWithIndexMetricsCollector = new TopicWithIndexMetricsCollector(
                    "totalEventsByStream", 10_000, metricsCollector
            );
        }

        @Override
        public void onFinishPrepare(EventsBatch<ElasticSender.ElasticPreparedData> batch) {
            Map<String, ElasticDocument> readyToSend = batch.getPreparedData().getReadyToSend();

            batch.events.forEach((topicPartition, events) ->
                    events.forEach(event -> {
                        ElasticDocument elasticDocument = readyToSend.get(EventUtil.extractStringId(event));
                        topicWithIndexMetricsCollector.markEvent(topicPartition, elasticDocument.index());
                    })
            );
        }
    }

    @Override
    public String getApplicationId() {
        return "sink.elastic";
    }

    @Override
    public String getApplicationName() {
        return "Hercules Elastic Sink";
    }
}
