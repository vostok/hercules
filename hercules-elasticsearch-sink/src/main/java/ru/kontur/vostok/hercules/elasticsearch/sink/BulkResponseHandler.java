package ru.kontur.vostok.hercules.elasticsearch.sink;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.http.HttpEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static ru.kontur.vostok.hercules.util.throwable.ThrowableUtil.toUnchecked;

public final class BulkResponseHandler {

    public static class Result {

        public static final Result OK = new Result(0, false);

        private final int errorCount;
        private final boolean hasRetryableErrors;

        public Result(int errorCount, boolean hasRetryableErrors) {
            this.errorCount = errorCount;
            this.hasRetryableErrors = hasRetryableErrors;
        }

        public int getErrorCount() {
            return errorCount;
        }

        public boolean hasRetryableErrors() {
            return hasRetryableErrors;
        }
    }

    private static final Set<String> RETRYABLE_ERRORS_CODES = new HashSet<>(Arrays.asList(
            "process_cluster_event_timeout_exception",
            "es_rejected_execution_exception"
    ));

    private static final Logger LOGGER = LoggerFactory.getLogger(BulkResponseHandler.class);

    private static final JsonFactory FACTORY = new JsonFactory();
    private static final ObjectMapper MAPPER = new ObjectMapper(FACTORY);

    // TODO: Replace with a good parser
    public static Result process(HttpEntity httpEntity) {
        return toUnchecked(() -> {
            int errorCount = 0;
            boolean hasRetryableErrors = false;
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
                    if (processError(MAPPER.readTree(parser), currentId, currentIndex)) {
                        hasRetryableErrors = true;
                    }
                    errorCount++;
                }
            }
            return new Result(errorCount, hasRetryableErrors);
        });
    }

    /**
     * Process error JSON node
     *
     * @param errorNode JSON node with error data
     * @param id event id
     * @param index index
     * @return is error retryable
     * @throws IOException
     */
    private static boolean processError(TreeNode errorNode, String id, String index) throws IOException {
        if (errorNode instanceof ObjectNode) {
            ObjectNode error = (ObjectNode) errorNode;
            JsonNode nestedError = error.get("caused_by");
            if (Objects.nonNull(nestedError)) {
                return processError(nestedError, id, index);
            } else {
                String type = Optional.ofNullable(error.get("type")).map(JsonNode::asText).orElse("");
                String reason = Optional.ofNullable(error.get("reason")).map(JsonNode::asText).orElse("");
                LOGGER.error(String.format(
                        "Bulk processing error: index=%s, id=%s, type=%s, reason=%s",
                        index,
                        id,
                        type,
                        reason.replaceAll("[\\r\\n]+", " "))
                );
                return RETRYABLE_ERRORS_CODES.contains(type);
            }
        }
        return false;
    }

    private BulkResponseHandler() {
        /* static class */
    }
}
