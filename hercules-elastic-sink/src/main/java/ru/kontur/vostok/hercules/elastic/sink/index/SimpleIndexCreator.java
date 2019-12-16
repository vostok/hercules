package ru.kontur.vostok.hercules.elastic.sink.index;

import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.http.HttpStatusCodes;

import java.io.IOException;

/**
 * @author Gregory Koshelev
 */
public class SimpleIndexCreator implements IndexCreator {
    private static Logger LOGGER = LoggerFactory.getLogger(SimpleIndexCreator.class);

    private final RestClient restClient;

    public SimpleIndexCreator(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public boolean create(String index) {
        try {
            Response response = restClient.performRequest("PUT", "/" + index);
            //TODO: Process response body in case of error
            return response.getStatusLine().getStatusCode() == HttpStatusCodes.OK;
        } catch (IOException ex) {
            LOGGER.warn("Cannot create index due to exception", ex);
            return false;
        }
    }
}
