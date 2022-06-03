package ru.kontur.vostok.hercules.gate.client;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.gate.client.exception.BadRequestException;
import ru.kontur.vostok.hercules.gate.client.exception.HttpProtocolException;
import ru.kontur.vostok.hercules.gate.client.exception.UnavailableClusterException;
import ru.kontur.vostok.hercules.gate.client.exception.UnavailableHostException;
import ru.kontur.vostok.hercules.util.concurrent.ScheduledThreadPoolExecutorBuilder;
import ru.kontur.vostok.hercules.util.concurrent.Topology;
import ru.kontur.vostok.hercules.util.parameter.Parameter;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;
import ru.kontur.vostok.hercules.util.validation.IntegerValidators;

import java.io.Closeable;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Client for Hercules Gateway API
 *
 * @author Daniil Zhenikhov
 */
public class GateClient implements Closeable {
    private static final Logger LOGGER = LoggerFactory.getLogger(GateClient.class);

    private static final String PING = "/ping";
    private static final String SEND_ACK = "/stream/send";
    private static final String SEND_ASYNC = "/stream/sendAsync";

    /**
     * Prefix for Authorization HTTP-header for ordinary API-keys.
     * <p>
     * E.g. {@code Authorization: Hercules apiKey <ordinary_key>}.
     */
    private static final String ORDINARY_APIKEY_PREFIX = "Hercules apiKey ";

    private final CloseableHttpClient client;

    private final BlockingQueue<GreyListTopologyElement> greyList;
    private final Topology<String> whiteList;
    private final int greyListElementsRecoveryTimeMs;
    private final String authHeader;

    private final ScheduledExecutorService scheduler =
            new ScheduledThreadPoolExecutorBuilder()
                    .threadPoolSize(1)
                    .name("gate-client-topology-updater")
                    .daemon(false)
                    .dropDelayedTasksAfterShutdown()
                    .build();

    public GateClient(Properties properties, Topology<String> whiteList, String apiKey) {
        this(properties, createHttpClient(properties), whiteList, apiKey);
    }

    public GateClient(Properties properties, CloseableHttpClient client, Topology<String> whiteList, String apiKey) {
        this.greyListElementsRecoveryTimeMs = PropertiesUtil.get(Props.GREY_LIST_ELEMENTS_RECOVERY_TIME_MS, properties).get();
        this.client = client;
        this.whiteList = whiteList;
        this.greyList = new ArrayBlockingQueue<>(whiteList.size());

        scheduler.scheduleWithFixedDelay(this::updateTopology,
                greyListElementsRecoveryTimeMs,
                greyListElementsRecoveryTimeMs,
                TimeUnit.MILLISECONDS);
        authHeader = ORDINARY_APIKEY_PREFIX + apiKey;
    }

    /**
     * Request to {@value #PING}
     *
     * @param url Gate Url
     * @throws BadRequestException      throws if was error on client side: 4xx errors or http protocol errors
     * @throws UnavailableHostException throws if was error on server side: 5xx errors or connection errors
     */
    public void ping(String url) throws BadRequestException, UnavailableHostException, HttpProtocolException {
        sendToHost(url, urlParam -> {
            HttpGet httpGet = new HttpGet(urlParam + PING);
            return sendRequest(httpGet);
        });
    }

    /**
     * Request to {@value #SEND_ASYNC}
     *
     * @param url    Gate url
     * @param stream topic name in kafka
     * @param data   payload
     * @throws BadRequestException      throws if was error on client side: 4xx errors or http protocol errors
     * @throws UnavailableHostException throws if was error on server side: 5xx errors or connection errors
     */
    public void sendAsync(String url, String stream, final byte[] data)
            throws BadRequestException, UnavailableHostException, HttpProtocolException {
        sendToHost(url, urlParam -> {
            HttpPost httpPost = buildRequest(url, SEND_ASYNC, stream, data);
            return sendRequest(httpPost);
        });
    }

    /**
     * Request to {@value #SEND_ACK}
     *
     * @param url    Gate url
     * @param stream topic name in kafka
     * @param data   payload
     * @throws BadRequestException      throws if was error on client side: 4xx errors or http protocol errors
     * @throws UnavailableHostException throws if was error on server side: 5xx errors or connection errors
     */
    public void send(String url, String stream, final byte[] data)
            throws BadRequestException, UnavailableHostException, HttpProtocolException {
        sendToHost(url, urlParam -> {
            HttpPost httpPost = buildRequest(url, SEND_ACK, stream, data);
            return sendRequest(httpPost);
        });
    }

    /**
     * Request to {@value #PING}
     *
     * @param retryLimit count of attempt to send data to one of the <code>urls</code>' hosts
     * @throws BadRequestException         throws if was error on client side: 4xx errors or http protocol errors
     * @throws UnavailableClusterException throws if was error on addresses pool side: no one of address is unavailable
     */
    public void ping(int retryLimit)
            throws BadRequestException, UnavailableClusterException {
        sendToPool(retryLimit, this::ping);
    }

    /**
     * Request to {@value #SEND_ASYNC}
     *
     * @param retryLimit count of attempt to send data to one of the <code>urls</code>' hosts
     * @param stream     topic name in kafka
     * @param data       payload
     * @throws BadRequestException         throws if was error on client side: 4xx errors or http protocol errors
     * @throws UnavailableClusterException throws if was error on addresses pool side: no one of address is unavailable
     */
    public void sendAsync(int retryLimit, String stream, final byte[] data)
            throws BadRequestException, UnavailableClusterException {
        sendToPool(retryLimit, url -> sendAsync(url, stream, data));
    }

    /**
     * Request to {@value #SEND_ACK}
     *
     * @param retryLimit count of attempt to send data to one of the <code>urls</code>' hosts
     * @param stream     topic name in kafka
     * @param data       payload
     * @throws BadRequestException         throws if was error on client side: 4xx errors or http protocol errors
     * @throws UnavailableClusterException throws if was error on addresses pool side: no one of address is unavailable
     */
    public void send(int retryLimit, String stream, final byte[] data)
            throws BadRequestException, UnavailableClusterException {
        sendToPool(retryLimit, url -> send(url, stream, data));
    }

    /**
     * Request to {@value #PING}. Count of retry is <code>whitelist.size() + 1</code>
     *
     * @throws BadRequestException         throws if was error on client side: 4xx errors or http protocol errors
     * @throws UnavailableClusterException throws if was error on addresses pool side: no one of address is unavailable
     */
    public void ping()
            throws BadRequestException, UnavailableClusterException {
        ping(whiteList.size() + 1);
    }

    /**
     * Request to {@value #SEND_ASYNC}. Count of retry is <code>whitelist.size() + 1</code>
     *
     * @param stream topic name in kafka
     * @param data   payload
     * @throws BadRequestException         throws if was error on client side: 4xx errors or http protocol errors
     * @throws UnavailableClusterException throws if was error on addresses pool side: no one of address is unavailable
     */
    public void sendAsync(String stream, final byte[] data)
            throws BadRequestException, UnavailableClusterException {
        sendAsync(whiteList.size() + 1, stream, data);
    }

    /**
     * Request to {@value #SEND_ACK}. Count of retry is <code>whitelist.size() + 1</code>
     *
     * @param stream topic name in kafka
     * @param data   payload
     * @throws BadRequestException         throws if was error on client side: 4xx errors or http protocol errors
     * @throws UnavailableClusterException throws if was error on addresses pool side: no one of address is unavailable
     */
    public void send(String stream, final byte[] data)
            throws BadRequestException, UnavailableClusterException {
        send(whiteList.size() + 1, stream, data);
    }

    public void close() {
        scheduler.shutdown();

        try {
            if (!scheduler.awaitTermination(5_000L, TimeUnit.MILLISECONDS)) {
                LOGGER.warn("Scheduled thread pool did not terminate");
            }
        } catch (InterruptedException ex) {
            LOGGER.error("Error on stopping scheduler", ex);
        }

        try {
            client.close();
        } catch (IOException e) {
            LOGGER.error("Error while closing http client: " + e.getLocalizedMessage());
        }
    }

    /**
     * Strategy of updating urls in topology
     */
    private void updateTopology() {
        if (greyList.isEmpty()) {
            return;
        }

        for (int i = 0; i < greyList.size(); i++) {
            GreyListTopologyElement element = greyList.peek();
            if (System.currentTimeMillis() - element.getEntryTime() >= greyListElementsRecoveryTimeMs) {
                GreyListTopologyElement pollElement = greyList.poll();
                whiteList.add(pollElement.getUrl());
            } else {
                return;
            }
        }
    }

    //TODO: metrics

    /**
     * Strategy of sending data to addresses pool
     */
    private void sendToPool(int retryLimit, HerculesRequestSender sender)
            throws BadRequestException, UnavailableClusterException {

        for (int count = 0; count < retryLimit; count++) {

            if (whiteList.isEmpty()) {
                throw new UnavailableClusterException();
            }

            String url = whiteList.next();

            try {
                sender.send(url);
                return;
            } catch (HttpProtocolException | UnavailableHostException e) {
                if (!whiteList.remove(url)) {
                    continue;
                }
                if (!greyList.offer(new GreyListTopologyElement(url))) {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Send fails", e);
                    }
                    whiteList.add(url);
                }
            }
        }

        throw new UnavailableClusterException();
    }

    //TODO: metrics

    /**
     * Strategy of sending data to single host
     */
    private void sendToHost(String url, ApacheRequestSender sender)
            throws BadRequestException, UnavailableHostException, HttpProtocolException {
        try {
            int statusCode = sender.send(url);

            if (statusCode >= 400 && statusCode < 500) {
                throw new BadRequestException(statusCode);
            } else if (statusCode >= 500) {
                throw new UnavailableHostException(url);
            }
        } catch (ClientProtocolException e) {
            throw new HttpProtocolException(e);
        } catch (IOException e) {
            throw new UnavailableHostException(url, e);
        }
    }

    private int sendRequest(HttpUriRequest request) throws IOException {
        CloseableHttpResponse response = client.execute(request);
        int statusCode = response.getStatusLine().getStatusCode();
        response.close();
        return statusCode;
    }

    /**
     * Build http post request
     *
     * @param url    gateway url
     * @param action Command in Hercules Gateway
     * @param stream topic name in kafka
     * @param data   payload
     * @return formatted http post request
     */
    private HttpPost buildRequest(String url, String action, String stream, byte[] data) {
        HttpPost httpPost = new HttpPost(url + action + "?stream=" + stream);

        httpPost.addHeader(HttpHeaders.AUTHORIZATION, authHeader);

        HttpEntity entity = new ByteArrayEntity(data, ContentType.APPLICATION_OCTET_STREAM);
        httpPost.setEntity(entity);

        return httpPost;
    }

    /**
     * Tuning of {@link CloseableHttpClient}
     *
     * @param properties Properties
     * @return Customized http client
     */
    private static CloseableHttpClient createHttpClient(Properties properties) {
        int requestTimeout = PropertiesUtil.get(Props.REQUEST_TIMEOUT, properties).get();
        int connectionTimeout = PropertiesUtil.get(Props.CONNECTION_TIMEOUT, properties).get();
        int connectionCount = PropertiesUtil.get(Props.CONNECTION_COUNT, properties).get();

        RequestConfig requestConfig = RequestConfig
                .custom()
                .setSocketTimeout(requestTimeout)
                .setConnectTimeout(connectionTimeout)
                .setConnectionRequestTimeout(connectionTimeout)
                .build();

        return HttpClientBuilder
                .create()
                .setDefaultRequestConfig(requestConfig)
                .setMaxConnPerRoute(connectionCount)
                .setMaxConnTotal(connectionCount)
                .setRetryHandler(new DefaultHttpRequestRetryHandler(0, false))
                .build();
    }

    @FunctionalInterface
    private interface ApacheRequestSender {
        int send(String url) throws IOException;
    }

    @FunctionalInterface
    private interface HerculesRequestSender {
        void send(String url) throws BadRequestException, UnavailableHostException, HttpProtocolException;
    }

    private static class Props {
        static final Parameter<Integer> REQUEST_TIMEOUT =
                Parameter.integerParameter("requestTimeout")
                        .withDefault(GateClientDefaults.DEFAULT_TIMEOUT)
                        .withValidator(IntegerValidators.positive())
                        .build();

        static final Parameter<Integer> CONNECTION_TIMEOUT =
                Parameter.integerParameter("connectionTimeout")
                        .withDefault(GateClientDefaults.DEFAULT_TIMEOUT)
                        .withValidator(IntegerValidators.positive())
                        .build();

        static final Parameter<Integer> CONNECTION_COUNT =
                Parameter.integerParameter("connectionCount")
                        .withDefault(GateClientDefaults.DEFAULT_CONNECTION_COUNT)
                        .withValidator(IntegerValidators.positive())
                        .build();

        static final Parameter<Integer> GREY_LIST_ELEMENTS_RECOVERY_TIME_MS =
                Parameter.integerParameter("greyListElementsRecoveryTimeMs")
                        .withDefault(GateClientDefaults.DEFAULT_RECOVERY_TIME)
                        .withValidator(IntegerValidators.positive())
                        .build();
    }
}
