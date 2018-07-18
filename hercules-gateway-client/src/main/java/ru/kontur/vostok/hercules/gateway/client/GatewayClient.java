package ru.kontur.vostok.hercules.gateway.client;

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
 * Client for Hercules Gateway API}
 *
 * @author Daniil Zhenikhov
 */
public class GatewayClient implements Closeable {
    private final static String PING = "/ping";
    private final static String SEND_ACK = "/stream/send";
    private final static String SEND_ASYNC = "/stream/sendAsync";


    private final CloseableHttpClient client = build();

    public void ping(String url) {
        HttpGet httpGet = new HttpGet(url + PING);

        send(httpGet);
    }

    /**
     * Request to {@link #SEND_ASYNC}
     *
     * @param url Where request should be sent
     * @param apiKey key for sending
     * @param stream topic in kafka
     * @param data payload
     */
    public void sendAsync(String url, String apiKey, String stream, final byte[] data) {
        HttpPost httpPost = getRequest(url, apiKey, SEND_ASYNC, stream, data);

        try {
            client.execute(httpPost);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     *
     * @param url Where request should be sent
     * @param apiKey key for sending
     * @param stream topic in kafka
     * @param data payload
     */
    public void send(String url, String apiKey, String stream, final byte[] data) {
        HttpPost httpPost = getRequest(url, apiKey, SEND_ACK, stream, data);

        send(httpPost);
    }

    public void close() {
        try {
            client.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     *  try execute query and print stack trace if exception has been thrown
     *
     * @param request The request should be sent
     */
    private void send(HttpUriRequest request) {
        try {
            CloseableHttpResponse response = client.execute(request);
            if (HttpStatus.SC_OK != response.getStatusLine().getStatusCode()) {
                System.err.println("GatewayClient request fails with HTTP code " + response.getStatusLine().getStatusCode());
            }
            response.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Form http post request
     *
     * @param url Where request should be sent
     * @param apiKey key for sending
     * @param cmd Command in Hercules Gateway
     * @param stream topic in kafka
     * @param data payload
     * @return formatted http post request
     */
    private HttpPost getRequest(String url, String apiKey, String cmd, String stream, byte[] data) {
        HttpPost httpPost = new HttpPost(url + cmd + "?stream=" + stream);

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
    private CloseableHttpClient build() {
        RequestConfig requestConfig = RequestConfig
                .custom()
                .setSocketTimeout(3000)
                .setConnectTimeout(3000)
                .build();

        return HttpClientBuilder
                .create()
                .setDefaultRequestConfig(requestConfig)
                .setMaxConnPerRoute(1000)
                .setMaxConnTotal(1000)
                .build();
    }
}
