package ru.kontur.vostok.hercules.elastic.sink;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.elastic.sink.error.ElasticError;
import ru.kontur.vostok.hercules.elastic.sink.error.ElasticErrorMetrics;
import ru.kontur.vostok.hercules.elastic.sink.error.ErrorGroup;
import ru.kontur.vostok.hercules.elastic.sink.index.IndexPolicy;
import ru.kontur.vostok.hercules.elastic.sink.index.IndexResolver;
import ru.kontur.vostok.hercules.elastic.sink.metrics.IndicesMetricsCollector;
import ru.kontur.vostok.hercules.health.IMetricsCollector;
import ru.kontur.vostok.hercules.health.Meter;
import ru.kontur.vostok.hercules.json.Document;
import ru.kontur.vostok.hercules.json.DocumentWriter;
import ru.kontur.vostok.hercules.json.format.EventToJsonFormatter;
import ru.kontur.vostok.hercules.kafka.util.processing.BackendServiceFailedException;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.util.EventUtil;
import ru.kontur.vostok.hercules.sink.ProcessorStatus;
import ru.kontur.vostok.hercules.sink.parallel.sender.ParallelSender;
import ru.kontur.vostok.hercules.sink.parallel.sender.PreparedData;
import ru.kontur.vostok.hercules.util.Maps;
import ru.kontur.vostok.hercules.util.parameter.Parameter;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;
import ru.kontur.vostok.hercules.util.validation.IntegerValidators;
import ru.kontur.vostok.hercules.util.validation.ValidationResult;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

/**
 * @author Gregory Koshelev
 */
public class ElasticSender extends ParallelSender<ElasticSender.ElasticPreparedData> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ElasticSender.class);

    private static final int EXPECTED_EVENT_SIZE_BYTES = 2_048;
    private static final ValidationResult UNDEFINED_INDEX_VALIDATION_RESULT = ValidationResult.error("Undefined index");

    private final IndexResolver indexResolver;

    private final EventToJsonFormatter eventFormatter;

    private final ElasticClient elasticClient;

    private final int retryLimit;
    private final boolean retryOnUnknownErrors;
    private final boolean compressionGzipEnable;

    private final LeproserySender leproserySender;

    private final IndicesMetricsCollector totalEventsIndicesMetricsCollector;
    private final IndicesMetricsCollector nonRetryableEventsIndicesMetricsCollector;
    private final Meter droppedNonRetryableErrorsMeter;
    private final Meter indexValidationErrorsMeter;
    private final ElasticErrorMetrics elasticErrorMetrics;

    public ElasticSender(
            Properties properties,
            IndexPolicy indexPolicy,
            ElasticClient elasticClient,
            LeproserySender leproserySender,
            IMetricsCollector metricsCollector
    ) {
        super(properties, metricsCollector);

        Properties indexResolverProperties = PropertiesUtil.ofScope(properties, "elastic.index.resolver");
        List<IndexResolver> indexResolvers = PropertiesUtil.createClassInstanceList(indexResolverProperties, IndexResolver.class);
        if (indexResolvers.isEmpty()) {
            throw new IllegalArgumentException("None of index resolvers are defined");
        }
        this.indexResolver = IndexResolver.forPolicy(indexPolicy, indexResolvers);

        this.eventFormatter = new EventToJsonFormatter(PropertiesUtil.ofScope(properties, "elastic.format"));

        this.elasticClient = elasticClient;
        this.leproserySender = leproserySender;

        this.retryLimit = PropertiesUtil.get(Props.RETRY_LIMIT, properties).get();
        this.retryOnUnknownErrors = PropertiesUtil.get(Props.RETRY_ON_UNKNOWN_ERRORS, properties).get();
        this.compressionGzipEnable = PropertiesUtil.get(Props.COMPRESSION_GZIP_ENABLE, properties).get();

        this.totalEventsIndicesMetricsCollector = new IndicesMetricsCollector("totalEvents", 10_000, metricsCollector);
        this.nonRetryableEventsIndicesMetricsCollector = new IndicesMetricsCollector("nonRetryableEvents", 10_000, metricsCollector);
        this.droppedNonRetryableErrorsMeter = metricsCollector.meter("droppedNonRetryableErrors");
        this.indexValidationErrorsMeter = metricsCollector.meter("indexValidationErrors");
        this.elasticErrorMetrics = new ElasticErrorMetrics("bulkResponseHandler", metricsCollector);
    }

    @Override
    public ProcessorStatus ping() {
        return elasticClient.ping() ? ProcessorStatus.AVAILABLE : ProcessorStatus.UNAVAILABLE;
    }

    @Override
    public ElasticPreparedData prepare(List<Event> events) {
        Map<ElasticDocument, ValidationResult> nonRetryableErrorsMap = new HashMap<>(events.size());
        //event-id -> elastic document
        Map<String, ElasticDocument> readyToSend = new HashMap<>(events.size());
        for (Event event : events) {
            Optional<String> index = indexResolver.resolve(event);
            String nonNullIndex = index.orElse("null");
            totalEventsIndicesMetricsCollector.markEvent(nonNullIndex);
            Document jsonDocument = eventFormatter.format(event);
            ElasticDocument document = new ElasticDocument(EventUtil.extractStringId(event), nonNullIndex, jsonDocument);
            readyToSend.put(document.id(), document);

            if (index.isEmpty()) {
                indexValidationErrorsMeter.mark();
                nonRetryableErrorsMap.put(document, UNDEFINED_INDEX_VALIDATION_RESULT);
            }
        }

        byte[] dataToIndex = getDataToIndex(readyToSend, nonRetryableErrorsMap);
        return new ElasticPreparedData(Collections.unmodifiableMap(readyToSend), nonRetryableErrorsMap, dataToIndex);
    }

    private byte[] getDataToIndex(Map<String, ElasticDocument> readyToSend, Map<ElasticDocument, ValidationResult> nonRetryableErrorsMap) {
        try {
            ByteArrayOutputStream dataStream = new ByteArrayOutputStream((readyToSend.size() - nonRetryableErrorsMap.size()) * EXPECTED_EVENT_SIZE_BYTES);//TODO: Replace EXPECTED_EVENT_SIZE_BYTES with heuristic is depending on Hercules event size
            for (ElasticDocument document : readyToSend.values()) {
                if (!nonRetryableErrorsMap.containsKey(document)) {
                    writeEventToStream(dataStream, document.index(), document.id(), document.document());
                }
            }
            byte[] data = dataStream.toByteArray();
            return compressionGzipEnable ? elasticClient.compressData(data) : data;
        } catch (IOException e) {
            throw new RuntimeException("Error on write ElasticDocument", e);
        }
    }

    @Override
    public int send(ElasticPreparedData preparedData) throws BackendServiceFailedException {
        if (preparedData.getEventsCount() == 0) {
            return 0;
        }

        Map<String, ElasticDocument> readyToSend = preparedData.getReadyToSend();
        Map<ElasticDocument, ValidationResult> nonRetryableErrorsMap = preparedData.nonRetryableErrorsMap;

        try {
            int retryCount = retryLimit;
            do {
                ElasticResponseHandler.Result result = elasticClient.index(preparedData.dataToIndex, compressionGzipEnable);

                if (result.getTotalErrors() == 0) {
                    int droppedCount = errorsProcess(nonRetryableErrorsMap);

                    return preparedData.getEventsCount() - droppedCount;
                } else {
                    int nonRetryableErrorsBeforeProcessSize = nonRetryableErrorsMap.size();

                    resultProcess(result, readyToSend).forEach((eventId, validationResult) ->
                            nonRetryableErrorsMap.put(readyToSend.get(eventId), validationResult)
                    );

                    if (nonRetryableErrorsMap.size() > nonRetryableErrorsBeforeProcessSize) {
                        LOGGER.debug("Retry after add non retryable: " + nonRetryableErrorsMap.size() + ", readyToSend: " + readyToSend.size());
                        preparedData.dataToIndex = getDataToIndex(readyToSend, nonRetryableErrorsMap);
                    } else {
                        LOGGER.debug("Retry retryable: " + nonRetryableErrorsMap.size() + ", readyToSend: " + readyToSend.size());
                    }
                }
            } while (0 < retryCount--);

            throw new Exception("Have retryable errors in elasticsearch response");
        } catch (Exception ex) {
            throw new BackendServiceFailedException(ex);
        }
    }

    private Map<String, ValidationResult> resultProcess(ElasticResponseHandler.Result result,
                                                        Map<String, ElasticDocument> documents) {
        LOGGER.debug(
                "Error statistics (retryable/non retryable/unknown/total): {}/{}/{}/{}",
                result.getRetryableErrorCount(),
                result.getNonRetryableErrorCount(),
                result.getUnknownErrorCount(),
                result.getTotalErrors()
        );

        Map<String, ValidationResult> errorsMap = new HashMap<>(Maps.effectiveHashMapCapacity(result.getErrors().size()));
        for (ElasticError error : result.getErrors()) {
            String eventId = error.documentId();
            ErrorGroup group = error.group();
            if (ErrorGroup.NON_RETRYABLE.equals(group) || (ErrorGroup.UNKNOWN.equals(group) && !retryOnUnknownErrors)) {
                errorsMap.put(eventId, ValidationResult.error(error.details()));
            }
            elasticErrorMetrics.markError(documents.get(eventId).index(), group, error.type());
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

        if (isLeproseryEnable()) {
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

    private boolean isLeproseryEnable() {
        return leproserySender != null;
    }

    private void writeEventToStream(OutputStream stream, String index, String documentId, Document document) throws IOException {
        IndexToElasticJsonWriter.writeIndex(stream, index, documentId);
        stream.write('\n');
        DocumentWriter.writeTo(stream, document);
        stream.write('\n');
    }

    public static class ElasticPreparedData implements PreparedData {
        //event-id -> elastic document
        private final Map<String, ElasticDocument> readyToSend;
        private final Map<ElasticDocument, ValidationResult> nonRetryableErrorsMap;
        private byte[] dataToIndex;

        public ElasticPreparedData(
                Map<String, ElasticDocument> readyToSend,
                Map<ElasticDocument, ValidationResult> nonRetryableErrorsMap,
                byte[] dataToIndex
        ) {
            this.readyToSend = readyToSend;
            this.nonRetryableErrorsMap = nonRetryableErrorsMap;
            this.dataToIndex = dataToIndex;
        }

        public Map<String, ElasticDocument> getReadyToSend() {
            return readyToSend;
        }

        @Override
        public int getEventsCount() {
            return readyToSend.size();
        }
    }

    static class Props {
        static final Parameter<IndexPolicy> INDEX_POLICY = Parameter
                .enumParameter("elastic.index.policy", IndexPolicy.class).
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

        static final Parameter<Boolean> COMPRESSION_GZIP_ENABLE = Parameter
                .booleanParameter("elastic.client.compression.gzip.enable")
                .withDefault(ElasticClientDefaults.DEFAULT_COMPRESSION_GZIP_ENABLE)
                .build();
    }
}
