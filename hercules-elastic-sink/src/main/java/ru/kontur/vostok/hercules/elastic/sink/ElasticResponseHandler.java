package ru.kontur.vostok.hercules.elastic.sink;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.http.HttpEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.elastic.sink.error.ElasticError;
import ru.kontur.vostok.hercules.elastic.sink.error.ErrorGroup;
import ru.kontur.vostok.hercules.elastic.sink.index.IndexCreator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static ru.kontur.vostok.hercules.util.throwable.ThrowableUtil.toUnchecked;

public class ElasticResponseHandler {

    public static class Result {

        public static final Result OK = new Result(
                0,
                0,
                0,
                Collections.emptyList()
        );

        private final int retryableErrorCount;
        private final int nonRetryableErrorCount;
        private final int unknownErrorCount;
        private final List<ElasticError> errors;

        public Result(
                int retryableErrorCount,
                int nonRetryableErrorCount,
                int unknownErrorCount,
                List<ElasticError> errors) {
            this.retryableErrorCount = retryableErrorCount;
            this.nonRetryableErrorCount = nonRetryableErrorCount;
            this.unknownErrorCount = unknownErrorCount;
            this.errors = errors;
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

        /**
         * @return list of errors
         */
        public List<ElasticError> getErrors() {
            return errors;
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

    private static final String INDEX_NOT_FOUND_EXCEPTION = "index_not_found_exception";

    private static final Logger LOGGER = LoggerFactory.getLogger(ElasticResponseHandler.class);

    private static final JsonFactory FACTORY = new JsonFactory();
    private static final ObjectMapper MAPPER = new ObjectMapper(FACTORY);

    private final Set<String> redefinedExceptions;
    private final boolean shouldCreateIndexIfAbsent;
    private final IndexCreator indexCreator;

    public ElasticResponseHandler(Set<String> redefinedExceptions, boolean shouldCreateIndexIfAbsent, IndexCreator indexCreator) {
        this.redefinedExceptions = redefinedExceptions;
        this.shouldCreateIndexIfAbsent = shouldCreateIndexIfAbsent;
        this.indexCreator = indexCreator;
    }

    // TODO: Replace with a good parser
    public Result process(HttpEntity httpEntity) {
        return toUnchecked(() -> {
            int retryableErrorCount = 0;
            int nonRetryableErrorCount = 0;
            int unknownErrorCount = 0;
            List<ElasticError> errors = new ArrayList<>();

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
                    final ElasticError error = processError(MAPPER.readTree(parser), currentId, currentIndex);
                    errors.add(error);
                    switch (error.group()) {
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
                            throw new RuntimeException(String.format("Unsupported error group '%s'", error.group()));
                    }
                }
            }
            createIndicesIfNeeded(errors);

            return new Result(retryableErrorCount, nonRetryableErrorCount, unknownErrorCount, errors);
        });
    }

    /**
     * Classify the error are parsed from the JSON node
     *
     * @param errorNode the JSON node with error data
     * @param id        event id
     * @param index     index
     * @return the classified error
     */
    private ElasticError processError(TreeNode errorNode, String id, String index) {
        if (errorNode instanceof ObjectNode) {
            ObjectNode error = (ObjectNode) errorNode;
            LOGGER.debug("Original error: {}", error);

            final String type = Optional.ofNullable(error.get("type"))
                    .map(JsonNode::asText)
                    .orElse("");

            final String reason = Optional.ofNullable(error.get("reason"))
                    .map(JsonNode::asText)
                    .orElse("")
                    .replaceAll("[\\r\\n]+", " ");

            //TODO: Build "caused by" trace

            ErrorGroup group = getGroupForType(type);
            LOGGER.debug("Got error: group={}, index={}, id={}, type={}, reason={}", group, index, id, type, reason);
            return new ElasticError(group, type, index, id, error.toString());
        } else {
            String errorMessage = "Error node is not object node, cannot parse";
            LOGGER.warn(errorMessage);
            return new ElasticError(ErrorGroup.NON_RETRYABLE, "", index, id, errorMessage);
        }
    }

    private ErrorGroup getGroupForType(String type) {
        if (shouldCreateIndexIfAbsent && INDEX_NOT_FOUND_EXCEPTION.equals(type)) {
            return ErrorGroup.RETRYABLE;
        }
        if (redefinedExceptions.contains(type)) {
            LOGGER.info("Retryable error of type {} has been redefined as non-retryable", type);
            return ErrorGroup.NON_RETRYABLE;
        }
        if (RETRYABLE_ERRORS_CODES.contains(type)) {
            return ErrorGroup.RETRYABLE;
        }
        if (NON_RETRYABLE_ERRORS_CODES.contains(type)) {
            return ErrorGroup.NON_RETRYABLE;
        }
        return ErrorGroup.UNKNOWN;
    }

    private void createIndicesIfNeeded(List<ElasticError> errors) {
        if (!shouldCreateIndexIfAbsent) {
            return;
        }

        errors.stream().
                filter(e -> ErrorGroup.RETRYABLE.equals(e.group()) && INDEX_NOT_FOUND_EXCEPTION.equals(e.type())).
                map(ElasticError::index).
                distinct().
                forEach(this::createIndex);
    }

    private void createIndex(String index) {
        LOGGER.info("Index " + index + " not found, will create");
        if (indexCreator.create(index)) {
            LOGGER.info("Index " + index + " has been created, will retry indexing");
        } else {
            LOGGER.warn("Cannot create index " + index + ", will retry anyway");
        }
        indexCreator.waitForIndexReadiness(index);
    }
}
