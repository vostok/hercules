package ru.kontur.vostok.hercules.gate.client;

import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

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

    public void ping(String url) throws IOException {
        HttpGet httpGet = new HttpGet(url + PING);

        send(httpGet);
    }

    /**
     * Request to {@link #SEND_ASYNC}
     *
     * @param url    gateway url
     * @param apiKey key for sending
     * @param stream topic name in kafka
     * @param data   payload
     */
    public void sendAsync(String url, String apiKey, String stream, final byte[] data) throws IOException {
        HttpPost httpPost = buildRequest(url, apiKey, SEND_ASYNC, stream, data);

        client.execute(httpPost);
    }

    /**
     * @param url    gateway url
     * @param apiKey key for sending
     * @param stream topic name in kafka
     * @param data   payload
     */
    public void send(String url, String apiKey, String stream, final byte[] data) throws IOException {
        HttpPost httpPost = buildRequest(url, apiKey, SEND_ACK, stream, data);

        send(httpPost);
    }

    public void close() {
        try {
            client.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void send(HttpUriRequest request) throws IOException {
        CloseableHttpResponse response = client.execute(request);
        if (HttpStatus.SC_OK != response.getStatusLine().getStatusCode()) {
            System.err.println("GateClient request fails with HTTP code " + response.getStatusLine().getStatusCode());
        }
        response.close();
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
}
