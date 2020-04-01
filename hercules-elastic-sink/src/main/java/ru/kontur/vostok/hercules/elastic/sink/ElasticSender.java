package ru.kontur.vostok.hercules.elastic.sink;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.configuration.Scopes;
import ru.kontur.vostok.hercules.elastic.sink.index.IndexPolicy;
import ru.kontur.vostok.hercules.elastic.sink.index.IndexResolver;
import ru.kontur.vostok.hercules.health.Meter;
import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.kafka.util.processing.BackendServiceFailedException;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.sink.ProcessorStatus;
import ru.kontur.vostok.hercules.sink.Sender;
import ru.kontur.vostok.hercules.util.parameter.Parameter;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;
import ru.kontur.vostok.hercules.util.validation.IntegerValidators;
import ru.kontur.vostok.hercules.util.validation.ValidationResult;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import static ru.kontur.vostok.hercules.util.throwable.ThrowableUtil.toUnchecked;

/**
 * @author Gregory Koshelev
 */
public class ElasticSender extends Sender {
    private static final Logger LOGGER = LoggerFactory.getLogger(ElasticSender.class);

    private static final int EXPECTED_EVENT_SIZE_BYTES = 2_048;

    private final boolean mergePropertiesTagToRoot;

    private final IndexPolicy indexPolicy;
    private final IndexResolver indexResolver;

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

        this.mergePropertiesTagToRoot = PropertiesUtil.get(Props.MERGE_PROPERTIES_TAG_TO_ROOT, properties).get();

        this.indexPolicy = PropertiesUtil.get(Props.INDEX_POLICY, properties).get();
        this.indexResolver = IndexResolver.forPolicy(indexPolicy);

        this.client = new ElasticClient(PropertiesUtil.ofScope(properties, "elastic.client"), indexPolicy, metricsCollector);

        this.retryLimit = PropertiesUtil.get(Props.RETRY_LIMIT, properties).get();
        this.retryOnUnknownErrors = PropertiesUtil.get(Props.RETRY_ON_UNKNOWN_ERRORS, properties).get();

        this.leproseryEnable = PropertiesUtil.get(Props.LEPROSERY_ENABLE, properties).get();
        this.leproserySender = leproseryEnable
                ? new LeproserySender(PropertiesUtil.ofScope(properties, Scopes.LEPROSERY), metricsCollector, mergePropertiesTagToRoot)
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
        Map<EventWrapper, ValidationResult> nonRetryableErrorsMap = new HashMap<>(events.size());
        //event-id -> event-wrapper
        Map<String, EventWrapper> readyToSend = new HashMap<>(events.size());
        for (Event event : events) {
            Optional<String> index = indexResolver.resolve(event);
            String nonNullIndex = index.orElse("null");
            EventWrapper wrapper = new EventWrapper(event, nonNullIndex);
            totalEventsIndicesMetricsCollector.markEvent(nonNullIndex);
            if (index.isPresent()) {
                readyToSend.put(wrapper.getId(), wrapper);
            } else {
                indexValidationErrorsMeter.mark();
                nonRetryableErrorsMap.put(wrapper, ValidationResult.error("Event index is null"));
            }
        }

        try {
            if (!readyToSend.isEmpty()) {
                int retryCount = retryLimit;
                do {
                    ByteArrayOutputStream dataStream = new ByteArrayOutputStream(readyToSend.size() * EXPECTED_EVENT_SIZE_BYTES);//TODO: Replace EXPECTED_EVENT_SIZE_BYTES with heuristic is depending on Hercules event size
                    readyToSend.values().forEach(wrapper -> writeEventToStream(dataStream, wrapper));
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
    private int errorsProcess(Map<EventWrapper, ValidationResult> nonRetryableErrorsInfo) {
        if (nonRetryableErrorsInfo.isEmpty()) {
            return 0;
        }

        for (EventWrapper wrapper : nonRetryableErrorsInfo.keySet()) {
            nonRetryableEventsIndicesMetricsCollector.markEvent(wrapper.getIndex());
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
            nonRetryableErrorsInfo.forEach((wrapper, validationResult) ->
                    LOGGER.warn("Non retryable error info: id = {}, index = {}, reason = {}",
                            wrapper.getId(),
                            wrapper.getIndex(),
                            validationResult.error()));
            droppedNonRetryableErrorsMeter.mark(nonRetryableErrorsInfo.size());
            return nonRetryableErrorsInfo.size();
        }
    }

    private void writeEventToStream(ByteArrayOutputStream stream, EventWrapper wrapper) {
        toUnchecked(() -> {
            IndexToElasticJsonWriter.writeIndex(stream, wrapper.getIndex(), wrapper.getId());
            stream.write('\n');
            EventToElasticJsonWriter.writeEvent(stream, wrapper.getEvent(), mergePropertiesTagToRoot);
            stream.write('\n');
        });
    }

    private static class Props {
        static final Parameter<Boolean> MERGE_PROPERTIES_TAG_TO_ROOT = Parameter
                .booleanParameter("elastic.mergePropertiesTagToRoot")
                .withDefault(Boolean.FALSE)
                .build();

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
