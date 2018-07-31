package ru.kontur.vostok.hercules.elasticsearch.sink;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.http.HttpEntity;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

import static ru.kontur.vostok.hercules.util.throwable.ThrowableUtil.toUnchecked;

public final class BulkResponseHandler {

    private final static JsonFactory FACTORY = new JsonFactory();

    // TODO: Replace with a good streaming parser
    public static void process(HttpEntity httpEntity) {
        toUnchecked(() -> {
            JsonParser parser = FACTORY.createParser(httpEntity.getContent());
            ObjectMapper mapper = new ObjectMapper(FACTORY);

            String currentId = "";

            while (Objects.nonNull(parser.nextToken())) {
                if ("_id".equals(parser.getCurrentName())) {
                    currentId = parser.getValueAsString("");
                }
                if ("error".equals(parser.getCurrentName())) {
                    parser.nextToken(); // Skip name
                    processError(mapper.readTree(parser), currentId);
                }
            }
        });
    }

    private static void processError(TreeNode errorNode, String id) throws IOException {
        if (errorNode instanceof ObjectNode) {
            ObjectNode error = (ObjectNode) errorNode;
            JsonNode nestedError = error.get("caused_by");
            if (Objects.nonNull(nestedError)) {
                processError(nestedError, id);
            } else {
                // TODO: Format log when logging will be added
                String type = Optional.ofNullable(error.get("type")).map(JsonNode::asText).orElse("");
                String reason = Optional.ofNullable(error.get("reason")).map(JsonNode::asText).orElse("");
                System.out.println(String.format("Bulk processing error: id=%s, type=%s, reason=%s", id, type, reason).replaceAll("[\\r\\n]+", ""));
            }
        }
    }

    private BulkResponseHandler() {
    }
}