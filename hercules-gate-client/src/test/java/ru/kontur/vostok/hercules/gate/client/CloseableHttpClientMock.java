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
    private static final String _4xx_ADDR = "error_client_1" + PING_METHOD;
    private static final String _5xx_ADDR = "error_host_1" + PING_METHOD;
    private static final String _503_ADDR = "error_host_2" + PING_METHOD;
    private static final String CLIENT_PROTOCOL_EXC_ADDR = "error_client_2" + PING_METHOD;
    private static final String IOEXC_ADDR = "error_host_3" + PING_METHOD;

    private static CloseableHttpResponse _4xx;
    private static CloseableHttpResponse _5xx;
    private static CloseableHttpResponse _503;

    static {
        _4xx = Mockito.mock(CloseableHttpResponse.class);
        StatusLine statusLine4xx = Mockito.mock(StatusLine.class);
        _5xx = Mockito.mock(CloseableHttpResponse.class);
        StatusLine statusLine5xx = Mockito.mock(StatusLine.class);
        _503 = Mockito.mock(CloseableHttpResponse.class);
        StatusLine statusLine503 = Mockito.mock(StatusLine.class);

        Mockito.when(_4xx.getStatusLine()).thenReturn(statusLine4xx);
        Mockito.when(statusLine4xx.getStatusCode()).thenReturn(400);
        Mockito.when(_5xx.getStatusLine()).thenReturn(statusLine5xx);
        Mockito.when(statusLine5xx.getStatusCode()).thenReturn(500);
        Mockito.when(_503.getStatusLine()).thenReturn(statusLine503);
        Mockito.when(statusLine503.getStatusCode()).thenReturn(503);
    }

    @Override
    protected CloseableHttpResponse doExecute(HttpHost target, HttpRequest request, HttpContext context) throws IOException, ClientProtocolException {
        switch (request.getRequestLine().getUri()) {
            case _4xx_ADDR:
                return _4xx;
            case _5xx_ADDR:
                return _5xx;
            case _503_ADDR:
                return _503;
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
