package ru.kontur.vostok.hercules.sentry.client.impl.client;

import java.io.Closeable;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import ru.kontur.vostok.hercules.sentry.client.impl.client.retry.RetryStrategy;

/**
 * @author Aleksandr Yuferov
 */

public class RestTemplate {

    private final HttpClient client;
    private final RetryStrategy retryStrategy;

    private RestTemplate(HttpClient client, RetryStrategy retryStrategy) {
        this.client = client;
        this.retryStrategy = retryStrategy;
    }

    public static RestTemplateBuilder builder() {
        return new RestTemplateBuilder();
    }

    public <T> HttpResponse<T> execute(HttpRequest request, BodyHandler<T> bodyHandler) throws IOException, InterruptedException {
        int tryCount = 0;
        HttpResponse<T> response = null;
        do {
            if (response != null && response.body() instanceof Closeable) {
                ((Closeable) response.body()).close();
            }
            response = client.send(request, bodyHandler);
        } while (retryStrategy.shouldRetry(tryCount++, response));
        return response;
    }

    public static class RestTemplateBuilder {
        private HttpClient client;
        private RetryStrategy retryStrategy;

        private RestTemplateBuilder() {}

        public RestTemplateBuilder withClient(HttpClient client) {
            this.client = client;
            return this;
        }

        public RestTemplateBuilder withRetryStrategy(RetryStrategy retryStrategy) {
            this.retryStrategy = retryStrategy;
            return this;
        }

        public RestTemplate build() {
            return new RestTemplate(client, retryStrategy);
        }
    }
}
