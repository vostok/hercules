package ru.kontur.vostok.hercules.gate.client;

import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.gate.client.exception.BadRequestException;
import ru.kontur.vostok.hercules.gate.client.exception.HttpProtocolException;
import ru.kontur.vostok.hercules.gate.client.exception.UnavailableClusterException;
import ru.kontur.vostok.hercules.gate.client.exception.UnavailableHostException;

import java.io.Closeable;
import java.io.IOException;
import java.util.Random;

/**
 * Client for Hercules Gateway API
 *
 * @author Daniil Zhenikhov
 */
public class GateClient implements Closeable {
    private static final Logger LOGGER = LoggerFactory.getLogger(GateClient.class);
    private static final Random RANDOM = new Random();

    private static final int TIMEOUT = 3000;
    private static final int CONNECTION_COUNT = 1000;

    private static final String PING = "/ping";
    private static final String SEND_ACK = "/stream/send";
    private static final String SEND_ASYNC = "/stream/sendAsync";

    private final CloseableHttpClient client;

    public GateClient(CloseableHttpClient client) {
        this.client = client;
    }

    public GateClient() {
        this.client = createHttpClient();
    }

    /**
     * Request to {@value #PING}
     *
     * @param url Gate Url
     * @throws BadRequestException throws if was error on client side: 4xx errors or http protocol errors
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
     * @param url Gate url
     * @param apiKey key for sending
     * @param stream topic name in kafka
     * @param data payload
     * @throws BadRequestException throws if was error on client side: 4xx errors or http protocol errors
     * @throws UnavailableHostException throws if was error on server side: 5xx errors or connection errors
     */
    public void sendAsync(String url, String apiKey, String stream, final byte[] data)
            throws BadRequestException, UnavailableHostException, HttpProtocolException {
        sendToHost(url, urlParam -> {
            HttpPost httpPost = buildRequest(url, apiKey, SEND_ASYNC, stream, data);
            client.execute(httpPost);
            return HttpStatus.SC_OK;
        });
    }

    /**
     * Request to {@value #SEND_ACK}
     *
     * @param url Gate url
     * @param apiKey key for sending
     * @param stream topic name in kafka
     * @param data payload
     * @throws BadRequestException throws if was error on client side: 4xx errors or http protocol errors
     * @throws UnavailableHostException throws if was error on server side: 5xx errors or connection errors
     */
    public void send(String url, String apiKey, String stream, final byte[] data)
            throws BadRequestException, UnavailableHostException, HttpProtocolException {
        sendToHost(url, urlParam -> {
            HttpPost httpPost = buildRequest(url, apiKey, SEND_ACK, stream, data);
            return sendRequest(httpPost);
        });
    }

    /**
     * Request to {@value #PING}
     *
     * @param urls addresses pool of gate
     * @param retryLimit count of attempt to send data to one of the <code>urls</code>' hosts
     * @throws BadRequestException throws if was error on client side: 4xx errors or http protocol errors
     * @throws UnavailableClusterException throws if was error on addresses pool side: no one of address is unavailable
     */
    public void ping(String[] urls, int retryLimit)
            throws BadRequestException, UnavailableClusterException {
        sendToPool(urls, retryLimit, this::ping);
    }

    /**
     * Request to {@value #SEND_ASYNC}
     *
     * @param urls addresses pool of gate
     * @param retryLimit count of attempt to send data to one of the <code>urls</code>' hosts
     * @param apiKey key for sending
     * @param stream topic name in kafka
     * @param data payload
     * @throws BadRequestException throws if was error on client side: 4xx errors or http protocol errors
     * @throws UnavailableClusterException throws if was error on addresses pool side: no one of address is unavailable
     */
    public void sendAsync(String[] urls, int retryLimit, String apiKey, String stream, final byte[] data)
            throws BadRequestException, UnavailableClusterException {
        sendToPool(urls, retryLimit, url -> sendAsync(url, apiKey, stream, data));
    }

    /**
     * Request to {@value #SEND_ACK}
     *
     * @param urls addresses pool of gate
     * @param retryLimit count of attempt to send data to one of the <code>urls</code>' hosts
     * @param apiKey key for sending
     * @param stream topic name in kafka
     * @param data payload
     * @throws BadRequestException throws if was error on client side: 4xx errors or http protocol errors
     * @throws UnavailableClusterException throws if was error on addresses pool side: no one of address is unavailable
     */
    public void send(String[] urls, int retryLimit, String apiKey, String stream, final byte[] data)
            throws BadRequestException, UnavailableClusterException {
        sendToPool(urls, retryLimit, url -> send(url, apiKey, stream, data));
    }

    /**
     * Request to {@value #PING}. Count of retry is <code>urls.length + 1</code>
     *
     * @param urls addresses pool of gate
     * @throws BadRequestException throws if was error on client side: 4xx errors or http protocol errors
     * @throws UnavailableClusterException throws if was error on addresses pool side: no one of address is unavailable
     */
    public void ping(String[] urls)
            throws BadRequestException, UnavailableClusterException {
        ping(urls, urls.length + 1);
    }

    /**
     * Request to {@value #SEND_ASYNC}. Count of retry is <code>urls.length + 1</code>
     *
     * @param urls addresses pool of gate
     * @param apiKey key for sending
     * @param stream topic name in kafka
     * @param data payload
     * @throws BadRequestException throws if was error on client side: 4xx errors or http protocol errors
     * @throws UnavailableClusterException throws if was error on addresses pool side: no one of address is unavailable
     */
    public void sendAsync(String[] urls, String apiKey, String stream, final byte[] data)
            throws BadRequestException, UnavailableClusterException {
        sendAsync(urls, urls.length + 1, apiKey, stream, data);
    }

    /**
     * Request to {@value #SEND_ACK}. Count of retry is <code>urls.length + 1</code>
     *
     * @param urls addresses pool of gate
     * @param apiKey key for sending
     * @param stream topic name in kafka
     * @param data payload
     * @throws BadRequestException throws if was error on client side: 4xx errors or http protocol errors
     * @throws UnavailableClusterException throws if was error on addresses pool side: no one of address is unavailable
     */
    public void send(String[] urls, String apiKey, String stream, final byte[] data)
            throws BadRequestException, UnavailableClusterException {
        send(urls, urls.length + 1, apiKey, stream, data);
    }

    public void close() {
        try {
            client.close();
        } catch (IOException e) {
            LOGGER.error("Error while closing http client: " + e.getLocalizedMessage());
        }
    }

    //TODO: metrics
    /**
     * Strategy of sending data to addresses pool
     */
    private void sendToPool(String[] urls, int retryLimit, HerculesRequestSender sender)
            throws BadRequestException, UnavailableClusterException {
        int seed = RANDOM.nextInt(urls.length);

        for (int i = seed, count = 0;
             count < retryLimit;
             count++, i = (i + 1) % urls.length) {

            try {
                sender.send(urls[i]);
                return;
            } catch (HttpProtocolException | UnavailableHostException e) {
                LOGGER.warn(e.getMessage());
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
     * @param apiKey key for sending
     * @param action Command in Hercules Gateway
     * @param stream topic name in kafka
     * @param data   payload
     * @return formatted http post request
     */
    private HttpPost buildRequest(String url, String apiKey, String action, String stream, byte[] data) {
        HttpPost httpPost = new HttpPost(url + action + "?stream=" + stream);

        httpPost.addHeader("apiKey", apiKey);

        HttpEntity entity = new ByteArrayEntity(data, ContentType.APPLICATION_OCTET_STREAM);
        httpPost.setEntity(entity);

        return httpPost;
    }

    /**
     * Tuning of {@link CloseableHttpClient}
     *
     * @return Customized http client
     */
    private CloseableHttpClient createHttpClient() {
        RequestConfig requestConfig = RequestConfig
                .custom()
                .setSocketTimeout(TIMEOUT)
                .setConnectTimeout(TIMEOUT)
                .build();

        return HttpClientBuilder
                .create()
                .setDefaultRequestConfig(requestConfig)
                .setMaxConnPerRoute(CONNECTION_COUNT)
                .setMaxConnTotal(CONNECTION_COUNT)
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
}
