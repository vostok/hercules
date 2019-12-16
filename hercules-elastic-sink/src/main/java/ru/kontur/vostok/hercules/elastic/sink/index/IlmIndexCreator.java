package ru.kontur.vostok.hercules.elastic.sink.index;

import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.http.HttpStatusCodes;

import java.io.IOException;
import java.util.Collections;

/**
 * @author Gregory Koshelev
 */
public class IlmIndexCreator implements IndexCreator {
    private static Logger LOGGER = LoggerFactory.getLogger(IlmIndexCreator.class);

    private final RestClient restClient;

    public IlmIndexCreator(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public boolean create(String index) {
        return createTemplate(index) && createIndex(index);
    }

    /**
     * TODO: replace with High Level REST Client
     *
     * <pre>{@code PUT _template/${index}
     * {
     *   "index_patterns": ["${index}-*"],
     *   "order": ${index.length()},
     *   "settings":  {
     *     "index.lifecycle.rollover_alias":  "${index}"
     *   }
     * }}</pre>
     *
     * @param index
     * @return
     */
    private boolean createTemplate(String index) {
        String stringBody =
                "{" +
                        "  \"index_patterns\": [\"" + index + "-*\"]," +
                        "  \"order\": " + index.length() + "," +
                        "  \"settings\": {" +
                        "    \"index.lifecycle.rollover_alias\": \"" + index + "\"" +
                        "  }" +
                        "}";
        HttpEntity body = new StringEntity(stringBody, ContentType.APPLICATION_JSON);

        try {
            Response response =
                    restClient.performRequest(
                            "PUT",
                            "/_template/" + index,
                            Collections.emptyMap(),
                            body);
            //TODO: Process response body in case of error
            return response.getStatusLine().getStatusCode() == HttpStatusCodes.OK;
        } catch (IOException ex) {
            LOGGER.warn("Cannot create index due to exception", ex);
            return false;
        }
    }

    /**
     * TODO: Replace with High Level REST Client
     *
     * <pre>{@code PUT ${index}-000001
     * {
     *   "aliases":  {
     *     "${index}":  {
     *       "is_write_index":  true
     *     }
     *   }
     * }}</pre>
     *
     * @param index
     * @return
     */
    private boolean createIndex(String index) {
        String stringBody =
                "{" +
                        "  \"aliases\": {" +
                        "    \"" + index + "\": {" +
                        "      \"is_write_index\": true" +
                        "    }" +
                        "  }" +
                        "}";
        HttpEntity body = new StringEntity(stringBody, ContentType.APPLICATION_JSON);

        try {
            Response response =
                    restClient.performRequest(
                            "PUT",
                            "/" + index + "-000001",
                            Collections.emptyMap(),
                            body);
            //TODO: Process response body in case of error
            return response.getStatusLine().getStatusCode() == HttpStatusCodes.OK;
        } catch (IOException ex) {
            LOGGER.warn("Cannot create index due to exception", ex);
            return false;
        }
    }
}
