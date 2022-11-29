package ru.kontur.vostok.hercules.sentry.client.impl.client.v7;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import ru.kontur.vostok.hercules.http.HttpStatusCodes;
import ru.kontur.vostok.hercules.sentry.client.impl.client.RestTemplate;
import ru.kontur.vostok.hercules.sentry.client.impl.client.cache.DsnCache;
import ru.kontur.vostok.hercules.sentry.client.impl.client.pagination.PaginationIterator;
import ru.kontur.vostok.hercules.sentry.client.impl.client.retry.RetryStrategies;
import ru.kontur.vostok.hercules.sentry.client.impl.client.v7.model.Dsn;
import ru.kontur.vostok.hercules.util.routing.SentryDestination;

/**
 * @author Aleksandr Yuferov
 */
public class DsnFetcherClient {

    private final RestTemplate restTemplate;
    private final ObjectReader reader;
    private final URI baseUri;
    private final String authToken;
    private DsnCache dsnCache;

    private DsnFetcherClient(
            RestTemplate restTemplate,
            ObjectReader reader,
            URI baseUri,
            String authToken,
            DsnCache dsnCache) {
        this.restTemplate = restTemplate;
        this.reader = reader;
        this.baseUri = baseUri;
        this.authToken = authToken;
        this.dsnCache = dsnCache;
    }

    public static DsnFetcherClientBuilder builder() {
        return new DsnFetcherClientBuilder();
    }

    public Dsn fetchDsn(SentryDestination destination) throws IOException, InterruptedException {
        URI requestBaseUri = baseUri.resolve("/api/0/projects/" + destination.organization() + "/" + destination.project() + "/keys/");
        var pageIterator = new PaginationIterator<>(requestBaseUri, uri -> {
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .GET()
                    .header("Authorization", "Bearer " + authToken)
                    .build();
            return restTemplate.execute(request, BodyHandlers.ofInputStream());
        });

        while (pageIterator.hasNext()) {
            HttpResponse<InputStream> currentPage = pageIterator.next();
            try (InputStream bodyIs = currentPage.body()) {
                if (currentPage.statusCode() == HttpStatusCodes.NOT_FOUND) {
                    return Dsn.unavailable();
                }
                if (currentPage.statusCode() != HttpStatusCodes.OK) {
                    throw new IOException("cannot connect to Sentry to fetch DSN for \""
                            + destination.organization() + "." + destination.project() + "\"");
                }
                try (MappingIterator<DsnDescriptor> dsnIterator = reader.readValues(bodyIs)) {
                    while (dsnIterator.hasNext()) {
                        DsnDescriptor descriptor = dsnIterator.next();

                        if (descriptor.active) {
                            return new Dsn(
                                    descriptor.publicKey,
                                    descriptor.projectId,
                                    dsn -> dsnCache.invalidate(destination));
                        }
                    }
                }
            }
        }

        return Dsn.unavailable();
    }

    public void setVault(DsnCache vault) {
        this.dsnCache = vault;
    }

    public static class DsnDescriptor {

        @JsonProperty("public")
        private String publicKey;
        @JsonProperty("isActive")
        private boolean active;
        @JsonProperty("projectId")
        private String projectId;
    }

    public static class DsnFetcherClientBuilder {

        private String sentryUrl;
        private String authToken;

        public DsnFetcherClientBuilder() {
        }

        public DsnFetcherClientBuilder withBaseUri(
                String sentryUrl) {
            this.sentryUrl = sentryUrl;
            return this;
        }

        public DsnFetcherClientBuilder withAuthToken(
                String authToken) {
            this.authToken = authToken;
            return this;
        }

        public DsnFetcherClient build() {
            HttpClient httpClient = HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_1_1)
                    .build();
            RestTemplate restTemplate = RestTemplate.builder()
                    .withClient(httpClient)
                    .withRetryStrategy(RetryStrategies.noRetry())
                    .build();
            ObjectMapper objectMapper = new ObjectMapper()
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                    .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                    .registerModule(new JavaTimeModule())
                    .setSerializationInclusion(JsonInclude.Include.NON_NULL);
            ObjectReader reader = objectMapper.readerFor(DsnFetcherClient.DsnDescriptor.class);
            URI baseUri = URI.create(sentryUrl);

            return new DsnFetcherClient(restTemplate, reader, baseUri, authToken, null);
        }
    }
}
