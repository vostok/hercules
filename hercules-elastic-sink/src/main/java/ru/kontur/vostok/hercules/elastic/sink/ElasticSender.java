package ru.kontur.vostok.hercules.elastic.sink;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.configuration.Scopes;
import ru.kontur.vostok.hercules.elastic.sink.index.IndexPolicy;
import ru.kontur.vostok.hercules.elastic.sink.index.IndexResolver;
import ru.kontur.vostok.hercules.health.Meter;
import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.json.Document;
import ru.kontur.vostok.hercules.json.DocumentWriter;
import ru.kontur.vostok.hercules.json.format.EventToJsonFormatter;
import ru.kontur.vostok.hercules.kafka.util.processing.BackendServiceFailedException;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.util.EventUtil;
import ru.kontur.vostok.hercules.sink.ProcessorStatus;
import ru.kontur.vostok.hercules.sink.Sender;
import ru.kontur.vostok.hercules.util.parameter.Parameter;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;
import ru.kontur.vostok.hercules.util.validation.IntegerValidators;
import ru.kontur.vostok.hercules.util.validation.ValidationResult;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

/**
 * @author Gregory Koshelev
 */
public class ElasticSender extends Sender {
    private static final Logger LOGGER = LoggerFactory.getLogger(ElasticSender.class);

    private static final int EXPECTED_EVENT_SIZE_BYTES = 2_048;
    private static final ValidationResult UNDEFINED_INDEX_VALIDATION_RESULT = ValidationResult.error("Undefined index");

    private final IndexPolicy indexPolicy;
    private final IndexResolver indexResolver;

    private final EventToJsonFormatter eventFormatter;

    private final ElasticClient client;

    private final int retryLimit;
    private final boolean retryOnUnknownErrors;

    private final boolean leproseryEnable;
    private final LeproserySender leproserySender;

    private final IndicesMetricsCollector totalEventsIndicesMetricsCollector;
    private final IndicesMetricsCollector nonRetryableEventsIndicesMetricsCollector;
    private final Meter droppedNonRetryableErrorsMeter;
    private final Meter indexValidationErrorsMeter;

    public ElasticSender(Properties properties, MetricsCollector metricsCollector) {
        super(properties, metricsCollector);

        this.indexPolicy = PropertiesUtil.get(Props.INDEX_POLICY, properties).get();

        Properties indexResolverProperties = PropertiesUtil.ofScope(properties, "elastic.index.resolver");
        this.indexResolver = IndexResolver.forPolicy(indexPolicy, indexResolverProperties);

        this.eventFormatter = new EventToJsonFormatter(PropertiesUtil.ofScope(properties, "elastic.format"));

        this.client = new ElasticClient(PropertiesUtil.ofScope(properties, "elastic.client"), indexPolicy, metricsCollector);

        this.retryLimit = PropertiesUtil.get(Props.RETRY_LIMIT, properties).get();
        this.retryOnUnknownErrors = PropertiesUtil.get(Props.RETRY_ON_UNKNOWN_ERRORS, properties).get();

        this.leproseryEnable = PropertiesUtil.get(Props.LEPROSERY_ENABLE, properties).get();
        this.leproserySender = leproseryEnable
                ? new LeproserySender(PropertiesUtil.ofScope(properties, Scopes.LEPROSERY), metricsCollector)
                : null;

        this.totalEventsIndicesMetricsCollector = new IndicesMetricsCollector("totalEvents", 10_000, metricsCollector);
        this.nonRetryableEventsIndicesMetricsCollector = new IndicesMetricsCollector("nonRetryableEvents", 10_000, metricsCollector);
        this.droppedNonRetryableErrorsMeter = metricsCollector.meter("droppedNonRetryableErrors");
        this.indexValidationErrorsMeter = metricsCollector.meter("indexValidationErrors");
    }

    @Override
    public ProcessorStatus ping() {
        return client.ping() ? ProcessorStatus.AVAILABLE : ProcessorStatus.UNAVAILABLE;
    }

    @Override
    protected int send(List<Event> events) throws BackendServiceFailedException {
        if (events.size() == 0) {
            return 0;
        }

        int droppedCount;
        Map<ElasticDocument, ValidationResult> nonRetryableErrorsMap = new HashMap<>(events.size());
        //event-id -> elastic document
        Map<String, ElasticDocument> readyToSend = new HashMap<>(events.size());

        try {
            for (Event event : events) {
                Optional<String> index = indexResolver.resolve(event);
                String nonNullIndex = index.orElse("null");
                totalEventsIndicesMetricsCollector.markEvent(nonNullIndex);
                Document jsonDocument = eventFormatter.format(event);
                ElasticDocument document = new ElasticDocument(EventUtil.extractStringId(event), nonNullIndex, jsonDocument);
                if (index.isPresent()) {
                    readyToSend.put(document.id(), document);
                } else {
                    indexValidationErrorsMeter.mark();
                    nonRetryableErrorsMap.put(document, UNDEFINED_INDEX_VALIDATION_RESULT);
                }
            }

            if (!readyToSend.isEmpty()) {
                int retryCount = retryLimit;
                do {
                    ByteArrayOutputStream dataStream = new ByteArrayOutputStream(readyToSend.size() * EXPECTED_EVENT_SIZE_BYTES);//TODO: Replace EXPECTED_EVENT_SIZE_BYTES with heuristic is depending on Hercules event size
                    for (ElasticDocument document : readyToSend.values()) {
                        writeEventToStream(dataStream, document.index(), document.id(), document.document());
                    }

                    ElasticResponseHandler.Result result = client.index(dataStream.toByteArray());
                    if (result.getTotalErrors() != 0) {
                        resultProcess(result).forEach((eventId, validationResult) ->
                                nonRetryableErrorsMap.put(readyToSend.remove(eventId), validationResult));
                    } else {
                        readyToSend.clear();
                    }
                } while (!readyToSend.isEmpty() && 0 < retryCount--);

                if (!readyToSend.isEmpty()) {
                    throw new Exception("Have retryable errors in elasticsearch response");
                }
            }

            droppedCount = errorsProcess(nonRetryableErrorsMap);
        } catch (Exception ex) {
            throw new BackendServiceFailedException(ex);
        }

        return events.size() - droppedCount;
    }

    private Map<String, ValidationResult> resultProcess(ElasticResponseHandler.Result result) {
        LOGGER.info(
                "Error statistics (retryable/non retryable/unknown/total): {}/{}/{}/{}",
                result.getRetryableErrorCount(),
                result.getNonRetryableErrorCount(),
                result.getUnknownErrorCount(),
                result.getTotalErrors()
        );

        Map<String, ValidationResult> errorsMap = new HashMap<>(result.getErrors().size());
        for (Map.Entry<String, ErrorInfo> entry : result.getErrors().entrySet()) {
            String eventId = entry.getKey();
            ErrorInfo errorInfo = entry.getValue();
            ErrorType type = errorInfo.getType();
            if (type.equals(ErrorType.NON_RETRYABLE) || (type.equals(ErrorType.UNKNOWN) && !retryOnUnknownErrors)) {
                errorsMap.put(eventId, ValidationResult.error(errorInfo.getError()));
            }
        }
        return errorsMap;
    }

    /**
     * @return count of dropped events
     */
    private int errorsProcess(Map<ElasticDocument, ValidationResult> nonRetryableErrorsInfo) {
        if (nonRetryableErrorsInfo.isEmpty()) {
            return 0;
        }

        for (ElasticDocument document : nonRetryableErrorsInfo.keySet()) {
            nonRetryableEventsIndicesMetricsCollector.markEvent(document.index());
        }

        if (leproseryEnable) {
            try {
                leproserySender.convertAndSend(nonRetryableErrorsInfo);
                return 0;
            } catch (Exception ex) {
                LOGGER.warn("Failed to send non retryable events to leprosery", ex);
                droppedNonRetryableErrorsMeter.mark(nonRetryableErrorsInfo.size());
                return nonRetryableErrorsInfo.size();
            }
        } else {
            nonRetryableErrorsInfo.forEach((document, validationResult) ->
                    LOGGER.warn("Non retryable error info: id = {}, index = {}, reason = {}",
                            document.id(),
                            document.index(),
                            validationResult.error()));
            droppedNonRetryableErrorsMeter.mark(nonRetryableErrorsInfo.size());
            return nonRetryableErrorsInfo.size();
        }
    }

    private void writeEventToStream(ByteArrayOutputStream stream, String index, String documentId, Document document) throws IOException {
        IndexToElasticJsonWriter.writeIndex(stream, index, documentId);
        stream.write('\n');
        DocumentWriter.writeTo(stream, document);
        stream.write('\n');
    }

    private static class Props {
        static final Parameter<IndexPolicy> INDEX_POLICY =
                Parameter.enumParameter("elastic.index.policy", IndexPolicy.class).
                        withDefault(IndexPolicy.DAILY).
                        build();

        static final Parameter<Integer> RETRY_LIMIT = Parameter
                .integerParameter("retryLimit")
                .withDefault(3)
                .withValidator(IntegerValidators.nonNegative())
                .build();

        static final Parameter<Boolean> RETRY_ON_UNKNOWN_ERRORS = Parameter
                .booleanParameter("retryOnUnknownErrors")
                .withDefault(Boolean.FALSE)
                .build();

        static final Parameter<Boolean> LEPROSERY_ENABLE = Parameter
                .booleanParameter("leprosery.enable")
                .withDefault(false)
                .build();
    }
}
