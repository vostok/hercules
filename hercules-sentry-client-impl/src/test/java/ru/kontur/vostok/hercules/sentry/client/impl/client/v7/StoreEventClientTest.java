package ru.kontur.vostok.hercules.sentry.client.impl.client.v7;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient.Version;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;
import javax.net.ssl.SSLSession;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import ru.kontur.vostok.hercules.http.HttpStatusCodes;
import ru.kontur.vostok.hercules.sentry.client.impl.client.RestTemplate;
import ru.kontur.vostok.hercules.sentry.client.impl.client.compression.CompressionStrategies;
import ru.kontur.vostok.hercules.sentry.client.impl.client.compression.Compressor;
import ru.kontur.vostok.hercules.sentry.client.impl.client.compression.NoopCompressorAlgorithm;
import ru.kontur.vostok.hercules.sentry.client.impl.client.v7.model.Dsn;

/**
 * @author Tatyana Tokmyanina
 */
public class StoreEventClientTest {
    static final Dsn dsn = new Dsn("key", "projectId", null);
    ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .registerModule(new JavaTimeModule())
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);
    URI baseUri = URI.create("https://a.b.ru");
    @Test
    public void shouldReturnZeroIfSendSuccessfully() throws IOException, InterruptedException {
        RestTemplate restTemplate = mock(RestTemplate.class);
        HttpResponse<Object> response = new HttpResponse<>() {
            @Override
            public int statusCode() {
                return 200;
            }

            @Override
            public HttpRequest request() {
                return null;
            }

            @Override
            public Optional<HttpResponse<Object>> previousResponse() {
                return Optional.empty();
            }

            @Override
            public HttpHeaders headers() {
                return null;
            }

            @Override
            public Void body() {
                return null;
            }

            @Override
            public Optional<SSLSession> sslSession() {
                return Optional.empty();
            }

            @Override
            public URI uri() {
                return null;
            }

            @Override
            public Version version() {
                return null;
            }
        };
        when(restTemplate.execute(any(), any())).thenReturn(response);
        StoreEventClient client = StoreEventClient.builder()
                .withRestTemplate(restTemplate)
                .withBaseUri(baseUri)
                .withObjectMapper(objectMapper)
                .withCompressor(new Compressor(CompressionStrategies.never(), new NoopCompressorAlgorithm()))
                .build();
        Assert.assertEquals(0, client.send(dsn, null));
    }
    @Test
    public void shouldDiscardDsn() throws IOException, InterruptedException {
        int[] statusCodes = {HttpStatusCodes.UNAUTHORIZED, HttpStatusCodes.FORBIDDEN, HttpStatusCodes.NOT_FOUND};
        for (int statusCode : statusCodes) {
            RestTemplate restTemplate = mock(RestTemplate.class);
            HttpResponse<Object> response = new HttpResponse<>() {
                @Override
                public int statusCode() {
                    return statusCode;
                }

                @Override
                public HttpRequest request() {
                    return null;
                }

                @Override
                public Optional<HttpResponse<Object>> previousResponse() {
                    return Optional.empty();
                }

                @Override
                public HttpHeaders headers() {
                    return null;
                }

                @Override
                public Void body() {
                    return null;
                }

                @Override
                public Optional<SSLSession> sslSession() {
                    return Optional.empty();
                }

                @Override
                public URI uri() {
                    return null;
                }

                @Override
                public Version version() {
                    return null;
                }
            };
            when(restTemplate.execute(any(), any())).thenReturn(response);
            Dsn dsn = Mockito.mock(Dsn.class);
            StoreEventClient client = StoreEventClient.builder()
                    .withRestTemplate(restTemplate)
                    .withBaseUri(baseUri)
                    .withObjectMapper(objectMapper)
                    .withCompressor(new Compressor(CompressionStrategies.never(),
                            new NoopCompressorAlgorithm()))
                    .build();
            client.send(dsn, null);
            verify(dsn).discard();
        }
    }
    @Test
    public void shouldReturnStatusCode() throws IOException, InterruptedException {
        int statusCode= 400;
        RestTemplate restTemplate = mock(RestTemplate.class);
        HttpResponse<Object> response = new HttpResponse<>() {
            @Override
            public int statusCode() {
                return statusCode;
            }

            @Override
            public HttpRequest request() {
                return null;
            }

            @Override
            public Optional<HttpResponse<Object>> previousResponse() {
                return Optional.empty();
            }

            @Override
            public HttpHeaders headers() {
                return null;
            }

            @Override
            public Void body() {
                return null;
            }

            @Override
            public Optional<SSLSession> sslSession() {
                return Optional.empty();
            }

            @Override
            public URI uri() {
                return null;
            }

            @Override
            public Version version() {
                return null;
            }
        };
        when(restTemplate.execute(any(), any())).thenReturn(response);
        StoreEventClient client = StoreEventClient.builder()
                .withRestTemplate(restTemplate)
                .withBaseUri(baseUri)
                .withObjectMapper(objectMapper)
                .withCompressor(new Compressor(CompressionStrategies.never(), new NoopCompressorAlgorithm()))
                .build();
        Assert.assertEquals(statusCode, client.send(dsn, null));
    }
}
