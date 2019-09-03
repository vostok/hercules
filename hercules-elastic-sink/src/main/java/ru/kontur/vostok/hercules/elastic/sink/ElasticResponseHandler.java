package ru.kontur.vostok.hercules.elastic.sink;

import com.codahale.metrics.Meter;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.http.HttpEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.health.MetricsUtil;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static ru.kontur.vostok.hercules.util.throwable.ThrowableUtil.toUnchecked;

public class ElasticResponseHandler {

    public static class Result {

        public static final Result OK = new Result(
                0,
                0,
                0,
                Collections.emptyMap()
        );

        private final int retryableErrorCount;
        private final int nonRetryableErrorCount;
        private final int unknownErrorCount;
        private final  Map<String, ErrorInfo> errorInfoMap;

        public Result(
                int retryableErrorCount,
                int nonRetryableErrorCount,
                int unknownErrorCount,
                Map<String, ErrorInfo> errorInfoMap
        ) {
            this.retryableErrorCount = retryableErrorCount;
            this.nonRetryableErrorCount = nonRetryableErrorCount;
            this.unknownErrorCount = unknownErrorCount;
            this.errorInfoMap = errorInfoMap;
        }

        public int getRetryableErrorCount() {
            return retryableErrorCount;
        }

        public int getNonRetryableErrorCount() {
            return nonRetryableErrorCount;
        }

        public int getUnknownErrorCount() {
            return unknownErrorCount;
        }

        public int getTotalErrors() {
            return retryableErrorCount + nonRetryableErrorCount + unknownErrorCount;
        }

        public Map<String, ErrorInfo> getErrors() {
            return errorInfoMap;
        }
    }

    static final Set<String> RETRYABLE_ERRORS_CODES = new HashSet<>(Arrays.asList(
            "process_cluster_event_timeout_exception",
            "es_rejected_execution_exception",
            "cluster_block_exception",
            "unavailable_shards_exception",
            "timeout_exception",
            "master_not_discovered_exception",
            "connect_transport_exception",
            "primary_missing_action_exception",
            "concurrent_snapshot_execution_exception",
            "receive_timeout_transport_exception",
            "elasticsearch_timeout_exception",
            "no_shard_available_action_exception",
            "node_not_connected_exception",
            "node_disconnected_exception",
            "not_master_exception",
            "delay_recovery_exception"
    ));

    static final Set<String> NON_RETRYABLE_ERRORS_CODES = new HashSet<>(Arrays.asList(
            "illegal_argument_exception",
            "mapper_parsing_exception",
            "illegal_state_exception",
            "invalid_index_name_exception",
            "index_closed_exception",
            "invalid_alias_name_exception",
            "elasticsearch_parse_exception",
            "invalid_type_name_exception",
            "parsing_exception",
            "index_template_missing_exception",
            "search_parse_exception",
            "timestamp_parsing_exception",
            "invalid_index_template_exception",
            "invalid_snapshot_name_exception",
            "document_source_missing_exception",
            "resource_already_exists_exception",
            "type_missing_exception",
            "index_shard_snapshot_failed_exception",
            "dfs_phase_execution_exception",
            "execution_cancelled_exception",
            "elasticsearch_security_exception",
            "index_shard_restore_exception",
            "bind_http_exception",
            "reduce_search_phase_exception",
            "node_closed_exception",
            "snapshot_failed_engine_exception",
            "shard_not_found_exception",
            "not_serializable_transport_exception",
            "response_handler_failure_transport_exception",
            "index_creation_exception",
            "index_not_found_exception",
            "illegal_shard_routing_state_exception",
            "broadcast_shard_operation_failed_exception",
            "resource_not_found_exception",
            "action_transport_exception",
            "elasticsearch_generation_exception",
            "index_shard_started_exception",
            "search_context_missing_exception",
            "general_script_exception",
            "snapshot_creation_exception",
            "document_missing_exception",
            "snapshot_exception",
            "index_primary_shard_not_allocated_exception",
            "transport_exception",
            "search_exception",
            "mapper_exception",
            "snapshot_restore_exception",
            "index_shard_closed_exception",
            "recover_files_recovery_exception",
            "truncated_translog_exception",
            "recovery_failed_exception",
            "index_shard_relocated_exception",
            "node_should_not_connect_exception",
            "translog_corrupted_exception",
            "fetch_phase_execution_exception",
            "version_conflict_engine_exception",
            "engine_exception",
            "no_such_node_exception",
            "settings_exception",
            "send_request_transport_exception",
            "not_serializable_exception_wrapper",
            "alias_filter_parsing_exception",
            "gateway_exception",
            "index_shard_not_recovering_exception",
            "http_exception",
            "elasticsearch_exception",
            "snapshot_missing_exception",
            "failed_node_exception",
            "blob_store_exception",
            "incompatible_cluster_state_version_exception",
            "recovery_engine_exception",
            "uncategorized_execution_exception",
            "routing_missing_exception",
            "index_shard_restore_failed_exception",
            "repository_exception",
            "aggregation_execution_exception",
            "refresh_failed_engine_exception",
            "aggregation_initialization_exception",
            "no_node_available_exception",
            "illegal_index_shard_state_exception",
            "index_shard_snapshot_exception",
            "index_shard_not_started_exception",
            "search_phase_execution_exception",
            "action_not_found_transport_exception",
            "transport_serialization_exception",
            "remote_transport_exception",
            "engine_creation_failure_exception",
            "routing_exception",
            "index_shard_recovery_exception",
            "repository_missing_exception",
            "no_class_settings_exception",
            "bind_transport_exception",
            "aliases_not_found_exception",
            "index_shard_recovering_exception",
            "translog_exception",
            "retry_on_primary_exception",
            "query_phase_execution_exception",
            "repository_verification_exception",
            "invalid_aggregation_path_exception",
            "http_on_transport_exception",
            "search_context_exception",
            "search_source_builder_exception",
            "flush_failed_engine_exception",
            "circuit_breaking_exception",
            "strict_dynamic_mapping_exception",
            "retry_on_replica_exception",
            "failed_to_commit_cluster_state_exception",
            "query_shard_exception",
            "no_longer_primary_shard_exception",
            "script_exception",
            "status_exception",
            "task_cancelled_exception",
            "shard_lock_obtain_failed_exception",
            "unknown_named_object_exception",
            "too_many_buckets_exception"
    ));

    private static final Logger LOGGER = LoggerFactory.getLogger(ElasticResponseHandler.class);

    private static final String METRIC_PREFIX = "bulkResponseHandler";

    private static final JsonFactory FACTORY = new JsonFactory();
    private static final ObjectMapper MAPPER = new ObjectMapper(FACTORY);

    private final MetricsCollector metricsCollector;

    private final ConcurrentHashMap<String, Meter> errorTypesMeter = new ConcurrentHashMap<>();

    private final Meter retryableErrorsMeter;
    private final Meter nonRetryableErrorsMeter;
    private final Meter unknownErrorsMeter;


    public ElasticResponseHandler(final MetricsCollector metricsCollector) {
        this.metricsCollector = metricsCollector;

        this.retryableErrorsMeter = metricsCollector.meter(METRIC_PREFIX + ".retryableErrors");
        this.nonRetryableErrorsMeter = metricsCollector.meter(METRIC_PREFIX + ".nonRetryableErrors");
        this.unknownErrorsMeter = metricsCollector.meter(METRIC_PREFIX + ".unknownErrors");
    }

    // TODO: Replace with a good parser
    public Result process(HttpEntity httpEntity, Set<String> redefinedExceptions) {
        return toUnchecked(() -> {
            int retryableErrorCount = 0;
            int nonRetryableErrorCount = 0;
            int unknownErrorCount = 0;
            Map<String, ErrorInfo> errorInfos = new HashMap<>();

            JsonParser parser = FACTORY.createParser(httpEntity.getContent());

            String currentId = "";
            String currentIndex = "";

            while (Objects.nonNull(parser.nextToken())) {
                /*
                 * No errors in response, so processing can be skipped
                 */
                if ("errors".equals(parser.getCurrentName())) {
                    if (Boolean.FALSE.equals(parser.nextBooleanValue())) {
                        return Result.OK;
                    }
                }

                if ("_id".equals(parser.getCurrentName())) {
                    currentId = parser.getValueAsString("");
                }
                if ("_index".equals(parser.getCurrentName())) {
                    currentIndex = parser.getValueAsString("");
                }
                if ("error".equals(parser.getCurrentName())) {
                    parser.nextToken(); // Skip name
                    final ErrorInfo error = processError(MAPPER.readTree(parser), currentId, currentIndex, redefinedExceptions);
                    errorInfos.put(currentId, error);
                    switch (error.getType()) {
                        case RETRYABLE:
                            retryableErrorCount++;
                            break;
                        case NON_RETRYABLE:
                            nonRetryableErrorCount++;
                            break;
                        case UNKNOWN:
                            unknownErrorCount++;
                            break;
                        default:
                            throw new RuntimeException(String.format("Unsupported error type '%s'", error.getType()));
                    }
                }
            }

            retryableErrorsMeter.mark(retryableErrorCount);
            nonRetryableErrorsMeter.mark(nonRetryableErrorCount);
            unknownErrorsMeter.mark(unknownErrorCount);

            return new Result(retryableErrorCount, nonRetryableErrorCount, unknownErrorCount, errorInfos);
        });
    }

    /**
     * Process error JSON node
     *
     * @param errorNode           JSON node with error data
     * @param id                  event id
     * @param index               index
     * @param redefinedExceptions exceptions for overriding
     * @return error type, which determines retryability of the error
     */
    private ErrorInfo processError(TreeNode errorNode, String id, String index, Set<String> redefinedExceptions) {
        if (errorNode instanceof ObjectNode) {
            ObjectNode error = (ObjectNode) errorNode;
            LOGGER.warn("Original error: {}", error);

            final String type = Optional.ofNullable(error.get("type"))
                    .map(JsonNode::asText)
                    .orElse("");

            final String reason = Optional.ofNullable(error.get("reason"))
                    .map(JsonNode::asText)
                    .orElse("")
                    .replaceAll("[\\r\\n]+", " ");

            //TODO: Build "caused by" trace

            errorTypesMeter.computeIfAbsent(type, this::createMeter).mark();
            if (redefinedExceptions.contains(type)){
                LOGGER.warn("Retryable error which will be regarded as non-retryable: index={}, id={}, type={}, reason={}", index, id, type,reason);
                return new ErrorInfo(ErrorType.NON_RETRYABLE, id, index, reason);
            } else if (RETRYABLE_ERRORS_CODES.contains(type)) {
                LOGGER.warn("Retryable error: index={}, id={}, type={}, reason={}", index, id, type,reason);
                return new ErrorInfo(ErrorType.RETRYABLE, id, index, reason);
            } else if (NON_RETRYABLE_ERRORS_CODES.contains(type)) {
                LOGGER.warn("Non retryable error: index={}, id={}, type={}, reason={}", index, id, type,reason);
                return new ErrorInfo(ErrorType.NON_RETRYABLE, id, index, reason);
            } else {
                LOGGER.warn("Unknown error: index={}, id={}, type={}, reason={}", index, id, type, reason);
                return new ErrorInfo(ErrorType.UNKNOWN, id, index, reason);
            }
        } else {
            String errorMessage = "Error node is not object node, cannot parse";
            LOGGER.warn(errorMessage);
            return new ErrorInfo(ErrorType.NON_RETRYABLE, id, index, errorMessage);
        }
    }

    private Meter createMeter(final String errorType) {
        return metricsCollector.meter(METRIC_PREFIX + ".errorTypes." + MetricsUtil.sanitizeMetricName(errorType));
    }
}
