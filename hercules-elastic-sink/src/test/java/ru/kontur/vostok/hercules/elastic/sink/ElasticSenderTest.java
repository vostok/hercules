package ru.kontur.vostok.hercules.elastic.sink;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.kontur.vostok.hercules.elastic.sink.error.ElasticError;
import ru.kontur.vostok.hercules.elastic.sink.error.ErrorGroup;
import ru.kontur.vostok.hercules.elastic.sink.index.IndexPolicy;
import ru.kontur.vostok.hercules.health.NoOpMetricsCollector;
import ru.kontur.vostok.hercules.kafka.util.processing.BackendServiceFailedException;
import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.EventBuilder;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.protocol.util.EventUtil;
import ru.kontur.vostok.hercules.sink.ProcessorStatus;
import ru.kontur.vostok.hercules.uuid.UuidGenerator;

import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ElasticSenderTest {

    @Mock
    private ElasticClient elasticClient;

    @Test
    public void prepare() {
        ElasticSender elasticSender = getElasticSender();
        Event event = getEventBuilder()
                .tag("properties", Variant.ofContainer(Container.of("elk-index", Variant.ofString("my_project-my_subproject"))))
                .build();
        ElasticSender.ElasticPreparedData elasticPreparedData = elasticSender.prepare(List.of(event));

        assertEquals(1, elasticPreparedData.getReadyToSend().size());
    }

    @Test
    public void okSend() throws BackendServiceFailedException {
        ElasticSender elasticSender = getElasticSender();
        Event event = getEventBuilder()
                .tag("properties", Variant.ofContainer(Container.of("elk-index", Variant.ofString("my_project-my_subproject"))))
                .build();
        ElasticSender.ElasticPreparedData elasticPreparedData = elasticSender.prepare(List.of(event));

        when(elasticClient.index(any(), eq(true))).thenReturn(ElasticResponseHandler.Result.OK);
        int sended = elasticSender.send(elasticPreparedData);

        assertEquals(1, sended);
        assertEquals(1, elasticPreparedData.getReadyToSend().size());
    }

    @Test
    public void okSendAfterRetryableError() throws BackendServiceFailedException {
        ElasticSender elasticSender = getElasticSender();
        Event event = getEventBuilder()
                .tag("properties", Variant.ofContainer(Container.of("elk-index", Variant.ofString("my_project-my_subproject"))))
                .build();
        ElasticSender.ElasticPreparedData elasticPreparedData = elasticSender.prepare(List.of(event));

        String documentId = EventUtil.extractStringId(event);
        ElasticError timeoutException = new ElasticError(ErrorGroup.RETRYABLE, "timeout_exception",
                elasticPreparedData.getReadyToSend().get(documentId).index(), documentId, "");

        ElasticResponseHandler.Result result = new ElasticResponseHandler.Result(1, 0, 0, List.of(timeoutException));
        when(elasticClient.index(any(), eq(true)))
                .thenReturn(result)
                .thenReturn(ElasticResponseHandler.Result.OK);

        int sended = elasticSender.send(elasticPreparedData);
        assertEquals(1, sended);
        assertEquals(1, elasticPreparedData.getReadyToSend().size());
    }

    @Test
    public void errorSend_retryableError() {
        ElasticSender elasticSender = getElasticSender();
        Event event = getEventBuilder()
                .tag("properties", Variant.ofContainer(Container.of("elk-index", Variant.ofString("my_project-my_subproject"))))
                .build();
        ElasticSender.ElasticPreparedData elasticPreparedData = elasticSender.prepare(List.of(event));

        String documentId = EventUtil.extractStringId(event);
        ElasticError timeoutException = new ElasticError(ErrorGroup.RETRYABLE, "timeout_exception",
                elasticPreparedData.getReadyToSend().get(documentId).index(), documentId, "");

        ElasticResponseHandler.Result result = new ElasticResponseHandler.Result(1, 0, 0, List.of(timeoutException));
        when(elasticClient.index(any(), eq(true)))
                .thenReturn(result);

        assertThrows(BackendServiceFailedException.class, () -> elasticSender.send(elasticPreparedData));
    }

    @Test
    public void okSendAfterNotRetryableError() throws BackendServiceFailedException {
        ElasticSender elasticSender = getElasticSender();
        Event event = getEventBuilder()
                .tag("properties", Variant.ofContainer(Container.of("elk-index", Variant.ofString("my_project-my_subproject"))))
                .build();
        Event event2 = getEventBuilder()
                .tag("properties", Variant.ofContainer(Container.of("elk-index", Variant.ofString("my_project-my_subproject"))))
                .build();
        ElasticSender.ElasticPreparedData preparedData = elasticSender.prepare(List.of(event, event2));

        String documentId = EventUtil.extractStringId(event);
        ElasticError timeoutException = new ElasticError(ErrorGroup.NON_RETRYABLE, "mapper_parsing_exception",
                preparedData.getReadyToSend().get(documentId).index(), documentId, "");

        ElasticResponseHandler.Result result = new ElasticResponseHandler.Result(1, 0, 0, List.of(timeoutException));
        when(elasticClient.index(any(), eq(true)))
                .thenReturn(result)
                .thenReturn(ElasticResponseHandler.Result.OK);

        int sended = elasticSender.send(preparedData);
        assertEquals(1, sended);
        assertEquals(2, preparedData.getReadyToSend().size());
    }

    /**
     * Correct check none valid events to send after retry
     */
    @Test
    public void notSendEmptyBodyAfterRetry() throws BackendServiceFailedException {
        ElasticSender elasticSender = getElasticSender();
        Event event = getEventBuilder()
                .tag("properties", Variant.ofContainer(Container.of("elk-index", Variant.ofString("my_project-my_subproject"))))
                .build();

        ElasticSender.ElasticPreparedData preparedData = elasticSender.prepare(List.of(event));

        String documentId = EventUtil.extractStringId(event);
        ElasticError timeoutException = new ElasticError(ErrorGroup.NON_RETRYABLE, "mapper_parsing_exception",
                preparedData.getReadyToSend().get(documentId).index(), documentId, "");

        ElasticResponseHandler.Result result = new ElasticResponseHandler.Result(1, 0, 0, List.of(timeoutException));
        when(elasticClient.index(any(), eq(true)))
                .thenReturn(result);

        int sended = elasticSender.send(preparedData);

        verify(elasticClient, times(1)).index(any(), eq(true));
        assertEquals(0, sended);
    }

    /**
     * Correct check none valid events to send
     */
    @Test
    public void notSendEmptyBody() throws BackendServiceFailedException {
        ElasticSender elasticSender = getElasticSender();
        Event event = getEventBuilder()
                .tag("properties", Variant.ofContainer(Container.of("foo", Variant.ofString("bar"))))
                .build();
        ElasticSender.ElasticPreparedData elasticPreparedData = elasticSender.prepare(List.of(event));

        int sended = elasticSender.send(elasticPreparedData);

        verify(elasticClient, never()).index(any(), eq(true));
        assertEquals(0, sended);
    }

    private static EventBuilder getEventBuilder() {
        return EventBuilder.create().uuid(UuidGenerator.getClientInstance().next()).timestamp(1);
    }

    @Test
    public void ping() {
        ElasticSender elasticSender = getElasticSender();

        assertEquals(ProcessorStatus.UNAVAILABLE, elasticSender.ping());
        when(elasticClient.ping()).thenReturn(true);
        assertEquals(ProcessorStatus.AVAILABLE, elasticSender.ping());
    }

    @NotNull
    private ElasticSender getElasticSender() {
        Properties properties = new Properties();
        properties.put("elastic.index.resolver.0.class", "ru.kontur.vostok.hercules.elastic.sink.index.TagsIndexResolver");
        properties.put("elastic.index.resolver.0.props.tags", "properties/elk-index");
        properties.put("elastic.format.file", "resource://log-event.mapping");
        properties.put("elastic.client.compression.gzip.enable", "true");
        return new ElasticSender(properties, IndexPolicy.DAILY, elasticClient, null, new NoOpMetricsCollector());
    }
}