package ru.kontur.vostok.hercules.elastic.sink;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import ru.kontur.vostok.hercules.elastic.sink.index.IndexCreator;
import ru.kontur.vostok.hercules.elastic.sink.index.IndexPolicy;
import ru.kontur.vostok.hercules.health.AutoMetricStopwatch;
import ru.kontur.vostok.hercules.health.Meter;
import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.health.Timer;
import ru.kontur.vostok.hercules.http.HttpStatusCodes;
import ru.kontur.vostok.hercules.util.parameter.Parameter;
import ru.kontur.vostok.hercules.util.parameter.parsing.Parsers;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;
import ru.kontur.vostok.hercules.util.validation.IntegerValidators;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * @author Gregory Koshelev
 */
class ElasticClient {
    private final RestClient restClient;


    private final ElasticResponseHandler elasticResponseHandler;

    private final Timer elasticsearchRequestTimeTimer;
    private final Meter elasticsearchRequestErrorsMeter;

    ElasticClient(Properties properties, IndexPolicy policy, MetricsCollector metricsCollector) {
        final HttpHost[] hosts = PropertiesUtil.get(Props.HOSTS, properties).get();
        final int maxConnections = PropertiesUtil.get(Props.MAX_CONNECTIONS, properties).get();
        final int maxConnectionsPerRoute = PropertiesUtil.get(Props.MAX_CONNECTIONS_PER_ROUTE, properties).get();
        final int retryTimeoutMs = PropertiesUtil.get(Props.RETRY_TIMEOUT_MS, properties).get();
        final int connectionTimeout = PropertiesUtil.get(Props.CONNECTION_TIMEOUT_MS, properties).get();
        final int connectionRequestTimeout = PropertiesUtil.get(Props.CONNECTION_REQUEST_TIMEOUT_MS, properties).get();
        final int socketTimeout = PropertiesUtil.get(Props.SOCKET_TIMEOUT_MS, properties).get();
        this.restClient = RestClient.builder(hosts)
                .setMaxRetryTimeoutMillis(retryTimeoutMs)
                .setHttpClientConfigCallback(httpClientBuilder -> httpClientBuilder
                        .setMaxConnTotal(maxConnections)
                        .setMaxConnPerRoute(maxConnectionsPerRoute)
                )
                .setRequestConfigCallback(requestConfigBuilder -> requestConfigBuilder
                        .setConnectTimeout(connectionTimeout)
                        .setConnectionRequestTimeout(connectionRequestTimeout)
                        .setSocketTimeout(socketTimeout)
                )
                .build();

        Set<String> redefinedExceptions = new HashSet<>(Arrays.asList(PropertiesUtil.get(Props.REDEFINED_EXCEPTIONS, properties).get()));
        boolean indexCreationEnable = PropertiesUtil.get(Props.INDEX_CREATION_ENABLE, properties).get();
        IndexCreator indexCreator = IndexCreator.forPolicy(policy, restClient);
        this.elasticResponseHandler = new ElasticResponseHandler(redefinedExceptions, indexCreationEnable, indexCreator, metricsCollector);

        this.elasticsearchRequestTimeTimer = metricsCollector.timer("elasticsearchRequestTimeMs");
        this.elasticsearchRequestErrorsMeter = metricsCollector.meter("elasticsearchRequestErrors");
    }

    boolean ping() {
        try {
            Response response = restClient.performRequest("HEAD", "/", Collections.emptyMap());
            return response.getStatusLine().getStatusCode() == HttpStatusCodes.OK;
        } catch (Exception ex) {
            return false;
        }
    }

    ElasticResponseHandler.Result index(byte[] dataToIndex) {
        HttpEntity body = new ByteArrayEntity(dataToIndex, ContentType.APPLICATION_JSON);

        Response response;
        try (AutoMetricStopwatch requestTime = new AutoMetricStopwatch(elasticsearchRequestTimeTimer, TimeUnit.MILLISECONDS)) {
            response = restClient.performRequest("POST", "/_bulk", Collections.emptyMap(), body);
        } catch (IOException ex) {
            elasticsearchRequestErrorsMeter.mark();
            throw new RuntimeException(ex);
        }
        if (response.getStatusLine().getStatusCode() != HttpStatusCodes.OK) {
            elasticsearchRequestErrorsMeter.mark();
            throw new RuntimeException("Bad response");
        }
        return elasticResponseHandler.process(response.getEntity());
    }

    private static class Props {
        static final Parameter<HttpHost[]> HOSTS = Parameter
                .parameter("hosts",
                        Parsers.fromFunction(str ->
                                Stream.of(str.split(",")).map(HttpHost::create).toArray(HttpHost[]::new)))
                .build();

        static final Parameter<Integer> MAX_CONNECTIONS = Parameter
                .integerParameter("maxConnections")
                .withValidator(IntegerValidators.positive())
                .withDefault(RestClientBuilder.DEFAULT_MAX_CONN_TOTAL)
                .build();

        static final Parameter<Integer> MAX_CONNECTIONS_PER_ROUTE = Parameter
                .integerParameter("maxConnectionsPerRoute")
                .withValidator(IntegerValidators.positive())
                .withDefault(RestClientBuilder.DEFAULT_MAX_CONN_PER_ROUTE)
                .build();

        static final Parameter<Integer> RETRY_TIMEOUT_MS = Parameter
                .integerParameter("retryTimeoutMs")
                .withValidator(IntegerValidators.nonNegative())
                .withDefault(RestClientBuilder.DEFAULT_MAX_RETRY_TIMEOUT_MILLIS)
                .build();

        static final Parameter<Integer> CONNECTION_TIMEOUT_MS = Parameter
                .integerParameter("connectionTimeoutMs")
                .withValidator(IntegerValidators.nonNegative())
                .withDefault(RestClientBuilder.DEFAULT_CONNECT_TIMEOUT_MILLIS)
                .build();

        static final Parameter<Integer> CONNECTION_REQUEST_TIMEOUT_MS = Parameter
                .integerParameter("connectionRequestTimeoutMs")
                .withValidator(IntegerValidators.nonNegative())
                .withDefault(RestClientBuilder.DEFAULT_CONNECTION_REQUEST_TIMEOUT_MILLIS)
                .build();

        static final Parameter<Integer> SOCKET_TIMEOUT_MS = Parameter
                .integerParameter("socketTimeoutMs")
                .withValidator(IntegerValidators.nonNegative())
                .withDefault(RestClientBuilder.DEFAULT_SOCKET_TIMEOUT_MILLIS)
                .build();

        static final Parameter<String[]> REDEFINED_EXCEPTIONS = Parameter
                .stringArrayParameter("redefinedExceptions")
                .withDefault(new String[]{})
                .build();

        static final Parameter<Boolean> INDEX_CREATION_ENABLE =
                Parameter.booleanParameter("index.creation.enable").
                        withDefault(false).
                        build();
    }
}
