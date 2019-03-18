package ru.kontur.vostok.hercules.gate.client;

import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
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
    private static final String ERROR_4XX_ADDR = "error_client_1" + PING_METHOD;
    private static final String ERROR_5XX_ADDR = "error_host_1" + PING_METHOD;
    private static final String ERROR_503_ADDR = "error_host_2" + PING_METHOD;
    private static final String CLIENT_PROTOCOL_EXC_ADDR = "error_client_2" + PING_METHOD;
    private static final String IOEXC_ADDR = "error_host_3" + PING_METHOD;

    private static final CloseableHttpResponse ERROR_4XX;
    private static final CloseableHttpResponse ERROR_5XX;
    private static final CloseableHttpResponse ERROR_503;

    static {
        ERROR_4XX = Mockito.mock(CloseableHttpResponse.class);
        StatusLine statusLine4xx = Mockito.mock(StatusLine.class);
        ERROR_5XX = Mockito.mock(CloseableHttpResponse.class);
        StatusLine statusLine5xx = Mockito.mock(StatusLine.class);
        ERROR_503 = Mockito.mock(CloseableHttpResponse.class);
        StatusLine statusLine503 = Mockito.mock(StatusLine.class);

        Mockito.when(ERROR_4XX.getStatusLine()).thenReturn(statusLine4xx);
        Mockito.when(statusLine4xx.getStatusCode()).thenReturn(400);
        Mockito.when(ERROR_5XX.getStatusLine()).thenReturn(statusLine5xx);
        Mockito.when(statusLine5xx.getStatusCode()).thenReturn(500);
        Mockito.when(ERROR_503.getStatusLine()).thenReturn(statusLine503);
        Mockito.when(statusLine503.getStatusCode()).thenReturn(503);
    }

    @Override
    protected CloseableHttpResponse doExecute(HttpHost target, HttpRequest request, HttpContext context) throws IOException, ClientProtocolException {
        switch (request.getRequestLine().getUri()) {
            case ERROR_4XX_ADDR:
                return ERROR_4XX;
            case ERROR_5XX_ADDR:
                return ERROR_5XX;
            case ERROR_503_ADDR:
                return ERROR_503;
            case CLIENT_PROTOCOL_EXC_ADDR:
                throw new ClientProtocolException();
            case IOEXC_ADDR:
            default:
                throw new IOException();
        }
    }

    @Override
    public void close() throws IOException {

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
