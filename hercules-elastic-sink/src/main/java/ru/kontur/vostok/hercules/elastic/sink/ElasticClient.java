package ru.kontur.vostok.hercules.elastic.sink;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import ru.kontur.vostok.hercules.elastic.sink.index.IndexCreator;
import ru.kontur.vostok.hercules.elastic.sink.index.IndexPolicy;
import ru.kontur.vostok.hercules.health.AutoMetricStopwatch;
import ru.kontur.vostok.hercules.health.Meter;
import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.health.Timer;
import ru.kontur.vostok.hercules.http.HttpStatusCodes;
import ru.kontur.vostok.hercules.http.header.HttpHeaders;
import ru.kontur.vostok.hercules.util.parameter.Parameter;
import ru.kontur.vostok.hercules.util.parameter.parsing.Parsers;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;
import ru.kontur.vostok.hercules.util.validation.IntegerValidators;

/**
 * Elastic {@link RestClient} adapter client.
 *
 * @author Gregory Koshelev
 */
class ElasticClient {
    // FIXME: After upgrade to 8.x client replace reflection by RestClient#isRunning
    private static final Field INNER_CLIENT_FIELD = Arrays.stream(RestClient.class.getDeclaredFields())
            .filter(field -> "client".equals(field.getName()))
            .peek(field -> field.setAccessible(true))
            .findFirst()
            .orElseThrow();

    private final Properties properties;
    private RestClient restClient;
    private final ElasticResponseHandler elasticResponseHandler;
    private final Timer elasticsearchRequestTimeTimer;
    private final Meter elasticsearchRequestErrorsMeter;
    private final boolean compressionGzipEnable;

    ElasticClient(Properties properties, IndexPolicy policy, MetricsCollector metricsCollector) {
        this.properties = PropertiesUtil.copy(properties);
        recreateRestClient();

        Set<String> redefinedExceptions = new HashSet<>(Arrays.asList(PropertiesUtil.get(Props.REDEFINED_EXCEPTIONS, this.properties).get()));
        boolean indexCreationEnable = PropertiesUtil.get(Props.INDEX_CREATION_ENABLE, this.properties).get();
        IndexCreator indexCreator = IndexCreator.forPolicy(policy, restClient);
        this.elasticResponseHandler = new ElasticResponseHandler(redefinedExceptions, indexCreationEnable, indexCreator, metricsCollector);

        this.elasticsearchRequestTimeTimer = metricsCollector.timer("elasticsearchRequestTimeMs");
        this.elasticsearchRequestErrorsMeter = metricsCollector.meter("elasticsearchRequestErrors");

        this.compressionGzipEnable = PropertiesUtil.get(Props.COMPRESSION_GZIP_ENABLE, this.properties).get();
    }

    /**
     * Try to ping cluster.
     *
     * @return {@code true} if cluster answers the test request or {@code false} if request finished with error.
     */
    boolean ping() {
        try {
            ensureClientIsRunning();
            Request request = new Request("HEAD", "/");
            Response response = restClient.performRequest(request);
            return response.getStatusLine().getStatusCode() == HttpStatusCodes.OK;
        } catch (Exception ex) {
            return false;
        }
    }

    /**
     * Index given data.
     * <p/>
     * Expected data in ElasticSearch bulk request format. If property {@code compression.gzip.enable} has value {@code true} then the GZip compression will be
     * performed on data before sending.
     *
     * @param dataToIndex Data to index.
     * @return Result of index request.
     */
    ElasticResponseHandler.Result index(byte[] dataToIndex) {
        ensureClientIsRunning();
        Response response;
        try (AutoMetricStopwatch ignored = new AutoMetricStopwatch(elasticsearchRequestTimeTimer, TimeUnit.MILLISECONDS)) {
            HttpEntity httpEntity = createHttpEntity(dataToIndex);
            Request request = prepareBulkRequest(httpEntity);
            response = restClient.performRequest(request);
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

    private void ensureClientIsRunning() {
        try {
            // FIXME: After upgrade to 8.x client replace reflection by RestClient#isRunning
            var innerClient = (CloseableHttpAsyncClient) INNER_CLIENT_FIELD.get(restClient);
            if (!innerClient.isRunning()) {
                recreateRestClient();
            }
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    private Request prepareBulkRequest(HttpEntity entity) {
        Request request = new Request("POST", "/_bulk");
        request.setEntity(entity);
        RequestOptions.Builder options = request.getOptions().toBuilder();
        if (compressionGzipEnable) {
            options.addHeader(HttpHeaders.CONTENT_ENCODING, "gzip");
        }
        request.setOptions(options);
        return request;
    }

    private HttpEntity createHttpEntity(byte[] data) throws IOException {
        byte[] dataToSend = compressionGzipEnable ? compressData(data) : data;
        return new ByteArrayEntity(dataToSend, ContentType.APPLICATION_JSON);
    }

    private byte[] compressData(byte[] data) throws IOException {
        try (var compressed = new ByteArrayOutputStream(data.length); var stream = new GZIPOutputStream(compressed)) {
            stream.write(data);
            return compressed.toByteArray();
        }
    }

    private void recreateRestClient() {
        final HttpHost[] hosts = PropertiesUtil.get(Props.HOSTS, properties).get();
        final int maxConnections = PropertiesUtil.get(Props.MAX_CONNECTIONS, properties).get();
        final int maxConnectionsPerRoute = PropertiesUtil.get(Props.MAX_CONNECTIONS_PER_ROUTE, properties).get();
        final int retryTimeoutMs = PropertiesUtil.get(Props.RETRY_TIMEOUT_MS, properties).get();
        final int connectionTimeout = PropertiesUtil.get(Props.CONNECTION_TIMEOUT_MS, properties).get();
        final int connectionRequestTimeout = PropertiesUtil.get(Props.CONNECTION_REQUEST_TIMEOUT_MS, properties).get();
        final int socketTimeout = PropertiesUtil.get(Props.SOCKET_TIMEOUT_MS, properties).get();
        final ElasticAuth auth = new ElasticAuth(PropertiesUtil.ofScope(properties, "auth"));

        restClient = RestClient.builder(hosts)
                .setMaxRetryTimeoutMillis(retryTimeoutMs)
                .setHttpClientConfigCallback(httpClientBuilder -> httpClientBuilder
                        .setMaxConnTotal(maxConnections)
                        .setMaxConnPerRoute(maxConnectionsPerRoute)
                        .setDefaultCredentialsProvider(auth.credentialsProvider())
                )
                .setRequestConfigCallback(requestConfigBuilder -> requestConfigBuilder
                        .setConnectTimeout(connectionTimeout)
                        .setConnectionRequestTimeout(connectionRequestTimeout)
                        .setSocketTimeout(socketTimeout)
                )
                .build();
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
                .withDefault(ElasticClientDefaults.DEFAULT_MAX_CONNECTIONS)
                .build();

        static final Parameter<Integer> MAX_CONNECTIONS_PER_ROUTE = Parameter
                .integerParameter("maxConnectionsPerRoute")
                .withValidator(IntegerValidators.positive())
                .withDefault(ElasticClientDefaults.DEFAULT_MAX_CONNECTIONS_PER_ROUTE)
                .build();

        static final Parameter<Integer> RETRY_TIMEOUT_MS = Parameter
                .integerParameter("retryTimeoutMs")
                .withValidator(IntegerValidators.nonNegative())
                .withDefault(ElasticClientDefaults.DEFAULT_RETRY_TIMEOUT_MS)
                .build();

        static final Parameter<Integer> CONNECTION_TIMEOUT_MS = Parameter
                .integerParameter("connectionTimeoutMs")
                .withValidator(IntegerValidators.nonNegative())
                .withDefault(ElasticClientDefaults.DEFAULT_CONNECTION_TIMEOUT_MS)
                .build();

        static final Parameter<Integer> CONNECTION_REQUEST_TIMEOUT_MS = Parameter
                .integerParameter("connectionRequestTimeoutMs")
                .withValidator(IntegerValidators.nonNegative())
                .withDefault(ElasticClientDefaults.DEFAULT_CONNECTION_REQUEST_TIMEOUT_MS)
                .build();

        static final Parameter<Integer> SOCKET_TIMEOUT_MS = Parameter
                .integerParameter("socketTimeoutMs")
                .withValidator(IntegerValidators.nonNegative())
                .withDefault(ElasticClientDefaults.DEFAULT_SOCKET_TIMEOUT_MS)
                .build();

        static final Parameter<String[]> REDEFINED_EXCEPTIONS = Parameter
                .stringArrayParameter("redefinedExceptions")
                .withDefault(ElasticClientDefaults.DEFAULT_REDEFINED_EXCEPTIONS)
                .build();

        static final Parameter<Boolean> INDEX_CREATION_ENABLE =
                Parameter.booleanParameter("index.creation.enable").
                        withDefault(ElasticClientDefaults.DEFAULT_INDEX_CREATION_ENABLE).
                        build();

        static final Parameter<Boolean> COMPRESSION_GZIP_ENABLE = Parameter
                .booleanParameter("compression.gzip.enable")
                .withDefault(ElasticClientDefaults.DEFAULT_COMPRESSION_GZIP_ENABLE)
                .build();
    }
}
