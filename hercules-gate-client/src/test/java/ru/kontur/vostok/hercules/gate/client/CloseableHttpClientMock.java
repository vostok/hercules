package ru.kontur.vostok.hercules.gate.client;

import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;
import org.mockito.Mockito;

import java.io.IOException;

/**
 * @author Daniil Zhenikhov
 */
public class CloseableHttpClientMock extends CloseableHttpClient {
    private static final String PING_METHOD = "/ping";
    private static final String OK_200_ADDR = "ok_2xx" + PING_METHOD;
    private static final String ERROR_4XX_ADDR = "error_4xx" + PING_METHOD;
    private static final String ERROR_5XX_ADDR = "error_5xx" + PING_METHOD;

    private static final CloseableHttpResponse OK_200;
    private static final CloseableHttpResponse ERROR_4XX;
    private static final CloseableHttpResponse ERROR_5XX;

    static {
        OK_200 = Mockito.mock(CloseableHttpResponse.class);
        StatusLine statusLine200 = Mockito.mock(StatusLine.class);
        ERROR_4XX = Mockito.mock(CloseableHttpResponse.class);
        StatusLine statusLine4xx = Mockito.mock(StatusLine.class);
        ERROR_5XX = Mockito.mock(CloseableHttpResponse.class);
        StatusLine statusLine5xx = Mockito.mock(StatusLine.class);

        Mockito.when(OK_200.getStatusLine()).thenReturn(statusLine200);
        Mockito.when(statusLine200.getStatusCode()).thenReturn(200);
        Mockito.when(ERROR_4XX.getStatusLine()).thenReturn(statusLine4xx);
        Mockito.when(statusLine4xx.getStatusCode()).thenReturn(400);
        Mockito.when(ERROR_5XX.getStatusLine()).thenReturn(statusLine5xx);
        Mockito.when(statusLine5xx.getStatusCode()).thenReturn(500);
    }

    @Override
    protected CloseableHttpResponse doExecute(HttpHost target, HttpRequest request, HttpContext context) throws IOException {
        switch (request.getRequestLine().getUri()) {
            case OK_200_ADDR:
                return OK_200;
            case ERROR_4XX_ADDR:
                return ERROR_4XX;
            case ERROR_5XX_ADDR:
                return ERROR_5XX;
            default:
                throw new IOException();
        }
    }

    @Override
    public void close() {

    }

    @Override
    public HttpParams getParams() {
        return null;
    }

    @Override
    public ClientConnectionManager getConnectionManager() {
        return null;
    }
}
