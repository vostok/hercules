package ru.kontur.vostok.hercules.sentry.client.impl.client.v7;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import ru.kontur.vostok.hercules.http.HttpStatusCodes;
import ru.kontur.vostok.hercules.sentry.client.impl.client.RestTemplate;
import ru.kontur.vostok.hercules.sentry.client.impl.client.compression.Compressor;
import ru.kontur.vostok.hercules.sentry.client.impl.client.v7.model.Dsn;
import ru.kontur.vostok.hercules.sentry.client.impl.client.v7.model.SentryEvent;

/**
 * @author Aleksandr Yuferov
 */
public class StoreEventClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final URI baseUri;
    private final Compressor compressor;

    private StoreEventClient(
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
            URI baseUri,
            Compressor compressor) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.baseUri = baseUri;
        this.compressor = compressor;
    }

    public static StoreEventClientBuilder builder() {
        return new StoreEventClientBuilder();
    }

    public int send(Dsn dsn, SentryEvent event) throws IOException, InterruptedException {
        HttpRequest request = prepareRequest(dsn, event);
        HttpResponse<Void> response = restTemplate.execute(request, BodyHandlers.discarding());
        switch (response.statusCode()) {
            case HttpStatusCodes.OK:
                return 0;
            case HttpStatusCodes.UNAUTHORIZED:
            case HttpStatusCodes.FORBIDDEN:
            case HttpStatusCodes.NOT_FOUND:
                dsn.discard();
            default:
                return response.statusCode();
        }
    }

    private HttpRequest prepareRequest(Dsn dsn, SentryEvent event) throws IOException {
        HttpRequest.Builder requestBuilder = HttpRequest
                .newBuilder(baseUri.resolve("/api/" + dsn.getProjectId() + "/store/"))
                .header("Content-Type", "application/json")
                .header("X-Sentry-Auth", "Sentry sentry_version=7,sentry_client=hercules,sentry_key=" + dsn.getPublicKey());
        byte[] jsonData = objectMapper.writeValueAsBytes(event);
        byte[] body = compressor.processData(requestBuilder, jsonData);
        return requestBuilder.POST(BodyPublishers.ofByteArray(body))
                .build();
    }

    public static class StoreEventClientBuilder {
        private RestTemplate restTemplate;
        private ObjectMapper objectMapper;
        private URI baseUri;
        private Compressor compressor;

        private StoreEventClientBuilder(){}

        public StoreEventClientBuilder withRestTemplate(RestTemplate template) {
            this.restTemplate = template;
            return this;
        }
        public StoreEventClientBuilder withObjectMapper(ObjectMapper mapper) {
            this.objectMapper = mapper;
            return this;
        }
        public StoreEventClientBuilder withBaseUri(URI baseUri) {
            this.baseUri = baseUri;
            return this;
        }
        public StoreEventClientBuilder withCompressor(Compressor compressor) {
            this.compressor = compressor;
            return this;
        }

        public StoreEventClient build() {
            return new StoreEventClient(restTemplate, objectMapper, baseUri, compressor);
        }
    }
}
