package ru.kontur.vostok.hercules.sink.parallel;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import ru.kontur.vostok.hercules.application.Application;
import ru.kontur.vostok.hercules.application.Container;
import ru.kontur.vostok.hercules.configuration.Scopes;
import ru.kontur.vostok.hercules.health.IMetricsCollector;
import ru.kontur.vostok.hercules.kafka.util.KafkaConfigs;
import ru.kontur.vostok.hercules.kafka.util.consumer.Subscription;
import ru.kontur.vostok.hercules.sink.SinkContext;
import ru.kontur.vostok.hercules.sink.SinkProps;
import ru.kontur.vostok.hercules.sink.filter.EventFilter;
import ru.kontur.vostok.hercules.sink.metrics.SinkMetrics;
import ru.kontur.vostok.hercules.sink.parallel.sender.AbstractParallelSender;
import ru.kontur.vostok.hercules.sink.parallel.sender.PreparedData;
import ru.kontur.vostok.hercules.util.parameter.Parameter;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;

import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Parallel Sink starter.
 * Uses one KafkaConsumer and ParallelSenderStrategy with prepare and send executors.
 *
 * @author Innokentiy Krivonosov
 */
public class ParallelSinkDaemonStarter<T extends PreparedData> {
    private final AbstractParallelSender<T> parallelSender;
    private final EventsBatchListener<T> eventsBatchListener;
    private final String daemonId;
    private final IMetricsCollector metricsCollector;

    public ParallelSinkDaemonStarter(
            AbstractParallelSender<T> parallelSender,
            EventsBatchListener<T> eventsBatchListener,
            String daemonId,
            IMetricsCollector metricsCollector
    ) {
        this.parallelSender = parallelSender;
        this.eventsBatchListener = eventsBatchListener;
        this.daemonId = daemonId;
        this.metricsCollector = metricsCollector;
    }

    public void start(Properties properties, Container container) {
        Properties sinkProperties = PropertiesUtil.ofScope(properties, Scopes.SINK);
        Properties strategyProperties = PropertiesUtil.ofScope(sinkProperties, Scopes.STRATEGY);

        int batchSize = PropertiesUtil.get(SinkProps.BATCH_SIZE, sinkProperties).get();
        long batchByteSize = PropertiesUtil.get(Props.BATCH_BYTE_SIZE, strategyProperties).get();

        int offsetsQueueSize = PropertiesUtil.get(Props.OFFSETS_QUEUE_SIZE, sinkProperties).get();

        ConcurrentTopicPartitionQueues topicPartitionQueues = new TopicPartitionQueuesImpl();
        BlockingQueue<Map<TopicPartition, OffsetAndMetadata>> offsetsToCommitQueue = new ArrayBlockingQueue<>(offsetsQueueSize);

        CreateEventsBatchStrategy<T> createEventsBatchStrategy = new CreateEventsBatchStrategyImpl<>(
                strategyProperties, topicPartitionQueues, batchSize, batchByteSize
        );

        List<EventFilter> filters = EventFilter.from(PropertiesUtil.ofScope(sinkProperties, "filter"));

        SinkMetrics metrics = new SinkMetrics(metricsCollector);
        PrepareExecutor<T> prepareExecutor = new PrepareExecutorImpl<>(parallelSender, filters, metrics);
        SendExecutor<T> sendExecutor = new SendExecutorImpl<>(parallelSender, metrics);

        ParallelSenderStrategy<T> parallelSenderStrategy = new ParallelSenderStrategy<>(
                strategyProperties, createEventsBatchStrategy, prepareExecutor,
                sendExecutor, topicPartitionQueues, offsetsToCommitQueue, metrics, eventsBatchListener, batchByteSize
        );

        Subscription subscription = getSubscription(sinkProperties);
        Consumer<byte[], byte[]> kafkaConsumer = getKafkaConsumer(
                daemonId, sinkProperties, subscription, batchSize, metricsCollector
        );

        container.register(new ParallelEventConsumer(
                sinkProperties, subscription, kafkaConsumer, topicPartitionQueues, offsetsToCommitQueue,
                parallelSender, parallelSenderStrategy, batchSize, batchByteSize, metrics));

        container.register(parallelSenderStrategy);
    }

    private static Subscription getSubscription(Properties sinkProperties) {
        return Subscription.builder().
                include(PropertiesUtil.get(SinkProps.PATTERN, sinkProperties).get()).
                exclude(PropertiesUtil.get(SinkProps.PATTERN_EXCLUSIONS, sinkProperties).get()).
                build();
    }

    private Consumer<byte[], byte[]> getKafkaConsumer(
            String applicationId,
            Properties sinkProperties,
            Subscription subscription,
            int batchSize,
            IMetricsCollector metricsCollector
    ) {
        String consumerGroupId = PropertiesUtil.get(SinkProps.GROUP_ID, sinkProperties)
                .orEmpty(subscription.toGroupId(applicationId));
        Application.context().put(SinkContext.GROUP_ID, consumerGroupId);
        Application.context().put(SinkContext.SUBSCRIPTION, subscription.toString());

        Properties consumerProperties = PropertiesUtil.ofScope(sinkProperties, Scopes.CONSUMER);
        consumerProperties.put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroupId);
        String consumerId = Application.context().getInstanceId();
        consumerProperties.put(ConsumerConfig.CLIENT_ID_CONFIG, consumerId);
        consumerProperties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        consumerProperties.putIfAbsent(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, batchSize);
        consumerProperties.put(KafkaConfigs.METRICS_COLLECTOR_INSTANCE_CONFIG, metricsCollector);

        return getConsumer(consumerProperties);
    }

    Consumer<byte[], byte[]> getConsumer(Properties consumerProperties) {
        return new KafkaConsumer<>(consumerProperties, new ByteArrayDeserializer(), new ByteArrayDeserializer());
    }

    static class Props {
        static final Parameter<Integer> OFFSETS_QUEUE_SIZE =
                Parameter.integerParameter("offsetsQueueSize")
                        .withDefault(1000)
                        .build();

        static final Parameter<Long> BATCH_BYTE_SIZE =
                Parameter.longParameter("batchByteSize")
                        .withDefault(10_000_000L)
                        .build();
    }
}
