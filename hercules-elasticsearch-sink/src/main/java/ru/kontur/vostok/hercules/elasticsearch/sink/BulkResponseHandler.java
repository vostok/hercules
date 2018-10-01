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
import java.util.Objects;
import java.util.Optional;

import static ru.kontur.vostok.hercules.util.throwable.ThrowableUtil.toUnchecked;

public final class BulkResponseHandler {

    private final static Logger LOGGER = LoggerFactory.getLogger(BulkResponseHandler.class);

    private final static JsonFactory factory = new JsonFactory();
    private final static ObjectMapper mapper = new ObjectMapper(factory);

    // TODO: Replace with a good parser
    public static int process(HttpEntity httpEntity) {
        return toUnchecked(() -> {
            int errorCount = 0;
            JsonParser parser = factory.createParser(httpEntity.getContent());

            String currentId = "";
            String currentIndex = "";

            while (Objects.nonNull(parser.nextToken())) {
                /*
                 * No errors in response, so processing can be skipped
                 */
                if ("errors".equals(parser.getCurrentName())) {
                    if (Boolean.FALSE.equals(parser.nextBooleanValue())) {
                        return 0;
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
                    processError(mapper.readTree(parser), currentId, currentIndex);
                    errorCount++;
                }
            }
            return errorCount;
        });
    }

    private static void processError(TreeNode errorNode, String id, String index) throws IOException {
        if (errorNode instanceof ObjectNode) {
            ObjectNode error = (ObjectNode) errorNode;
            JsonNode nestedError = error.get("caused_by");
            if (Objects.nonNull(nestedError)) {
                processError(nestedError, id, index);
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
            }
        }
    }

    private BulkResponseHandler() {
    }
}
