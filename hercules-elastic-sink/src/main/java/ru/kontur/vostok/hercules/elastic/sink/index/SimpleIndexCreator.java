package ru.kontur.vostok.hercules.elastic.sink.index;

import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import ru.kontur.vostok.hercules.http.HttpStatusCodes;

import java.io.IOException;

/**
 * @author Gregory Koshelev
 */
public class SimpleIndexCreator extends IndexCreator {
    public SimpleIndexCreator(RestClient restClient) {
        super(restClient);
    }

    @Override
    public boolean create(String index) {
        try {
            Response response = restClient.performRequest("PUT", "/" + index);
            //TODO: Process response body in case of error
            return HttpStatusCodes.isSuccess(response.getStatusLine().getStatusCode());
        } catch (IOException ex) {
            LOGGER.warn("Cannot create index due to exception", ex);
            return false;
        }
    }
}
