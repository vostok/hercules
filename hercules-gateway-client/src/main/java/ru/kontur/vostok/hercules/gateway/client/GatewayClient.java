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

public class GatewayClient implements Closeable {
    private final static String PING = "/ping";
    private final static String SEND_ACK = "/stream/send";
    private final static String SEND_ASYNC = "/stream/sendAsync";


    private final CloseableHttpClient client = build();
    private final String url;
    private final String apiKey;

    public GatewayClient(String url, String apiKey) {
        this.url = url;
        this.apiKey = apiKey;
    }

    public void ping() {
        HttpGet httpGet = new HttpGet(url + PING);

        send(httpGet);
    }

    public void sendAsync(String stream, final byte[] data) {
        HttpPost httpPost = getRequest(SEND_ASYNC, stream, data);

        try {
            client.execute(httpPost);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void send(final String stream, final byte[] data) {
        HttpPost httpPost = getRequest(SEND_ACK, stream, data);

        send(httpPost);
    }

    public void close() {
        try {
            client.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

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

    private HttpPost getRequest(String api, String stream, byte[] data) {
        HttpPost httpPost = new HttpPost(url + api + "?stream=" + stream);

        httpPost.addHeader("apiKey", apiKey);

        HttpEntity entity = new ByteArrayEntity(data, ContentType.APPLICATION_OCTET_STREAM);
        httpPost.setEntity(entity);

        return httpPost;
    }

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
