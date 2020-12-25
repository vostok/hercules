package ru.kontur.vostok.hercules.elastic.sink.index;

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
public abstract class IndexCreator {
    protected static final Logger LOGGER = LoggerFactory.getLogger(IndexCreator.class);

    protected final RestClient restClient;

    protected IndexCreator(RestClient restClient) {
        this.restClient = restClient;
    }

    public abstract boolean create(String index);

    /**
     * In case of {@link IndexPolicy#DAILY}, an index name should end with date in following format:<br>
     * <pre>
     * YYYY.MM.DD, where
     *     YYYY - year
     *     MM   - month from 1 (January) to 12 (December)
     *     DD   - day of month started with 1
     * </pre>
     *
     * @param policy     the index policy
     * @param restClient REST client
     * @return index creator instance
     */
    public static IndexCreator forPolicy(IndexPolicy policy, RestClient restClient) {
        switch (policy) {
            case DAILY:
            case STATIC:
                return new SimpleIndexCreator(restClient);
            case ILM:
                return new IlmIndexCreator(restClient);
            default:
                throw new IllegalArgumentException("Unknown index policy " + policy);
        }
    }

    /**
     * Wait for an index readiness.
     *
     * @param index the index
     * @return {@code true} if the index is ready or a timeout has expired, return {@code false} in case of any errors
     */
    public boolean waitForIndexReadiness(String index) {
        try {
            Response response =
                    restClient.performRequest(
                            "GET",
                            "/_cluster/health/" + index + "?wait_for_active_shards=1",
                            Collections.emptyMap());
            //TODO: Check active shards count in case of success (0 means 'false')
            return HttpStatusCodes.isSuccess(response.getStatusLine().getStatusCode());
        } catch (IOException ex) {
            LOGGER.warn("Cannot check index status due to exception", ex);
            return false;
        }
    }
}
