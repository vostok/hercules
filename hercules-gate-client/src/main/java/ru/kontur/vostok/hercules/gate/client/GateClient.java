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
import ru.kontur.vostok.hercules.gate.client.url.strategy.RoundRobinUrlIterator;
import ru.kontur.vostok.hercules.gate.client.url.strategy.UrlIterator;

import java.io.Closeable;
import java.io.IOException;

/**
 * Client for Hercules Gateway API
 *
 * @author Daniil Zhenikhov
 */
public class GateClient implements Closeable {
    private static final int TIMEOUT = 3000;
    private static final int CONNECTION_COUNT = 1000;

    private static final String PING = "/ping";
    private static final String SEND_ACK = "/stream/send";
    private static final String SEND_ASYNC = "/stream/sendAsync";


    private final CloseableHttpClient client = createHttpClient();

    public int ping(String url) throws IOException {
        HttpGet httpGet = new HttpGet(url + PING);

        return sendRequest(httpGet);
    }

    public boolean ping(UrlIterator iterator) {
        return sendToPool(iterator, this::ping);
    }

    public boolean ping(String[] urls) {
        return ping(new RoundRobinUrlIterator(urls));
    }

    /**
     * Request to {@link #SEND_ASYNC}
     *
     * @param url    gateway url
     * @param apiKey key for sending
     * @param stream topic name in kafka
     * @param data   payload
     */
    public int sendAsync(String url, String apiKey, String stream, final byte[] data) throws IOException {
        HttpPost httpPost = buildRequest(url, apiKey, SEND_ASYNC, stream, data);
        client.execute(httpPost);
        return HttpStatus.SC_OK;
    }

    public boolean sendAsync(UrlIterator iterator, String apiKey, String stream, final byte[] data) {
        return sendToPool(iterator, url -> sendAsync(url, apiKey, stream, data));
    }

    public boolean sendAsync(String[] urls, String apiKey, String stream, final byte[] data) {
        return sendAsync(new RoundRobinUrlIterator(urls), apiKey, stream, data);
    }

    /**
     * @param url    gateway url
     * @param apiKey key for sending
     * @param stream topic name in kafka
     * @param data   payload
     */
    public int send(String url, String apiKey, String stream, final byte[] data) throws IOException {
        HttpPost httpPost = buildRequest(url, apiKey, SEND_ACK, stream, data);

        return sendRequest(httpPost);
    }

    public boolean send(UrlIterator iterator, String apiKey, String stream, final byte[] data) {
        return sendToPool(iterator, url -> send(url, apiKey, stream, data));
    }

    public boolean send(String[] urls, String apiKey, String stream, final byte[] data) {
        return send(new RoundRobinUrlIterator(urls), apiKey, stream, data);
    }

    public void close() {
        try {
            client.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * @param iterator iterator of urls
     * @param requestSender interface for sending data
     * @return if data has sent successfully then return <code>true</code> else <code>false</code>
     */
    private boolean sendToPool(UrlIterator iterator, RequestSender requestSender) {
        int statusCode;
        String url;

        while (iterator.hasNext()) {
            try {
                url = iterator.next();
                statusCode = requestSender.send(url);

                if (statusCode == HttpStatus.SC_OK) {
                    //TODO:metrics
                    return true;
                }
                else if (statusCode >= 400 && statusCode < 500) {
                    //TODO: metrics
                    return false;
                }
            } catch (ClientProtocolException e) {
                //TODO: may be case of bad address format
                //TODO: metrics
                return false;
            } catch (IOException ignored) {
            }
        }

        //TODO: metrics;
        return false;

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
    private interface RequestSender {
        int send(String url) throws IOException;
    }
}
