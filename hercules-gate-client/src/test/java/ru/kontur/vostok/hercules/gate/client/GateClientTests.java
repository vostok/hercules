package ru.kontur.vostok.hercules.gate.client;

import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import ru.kontur.vostok.hercules.gate.client.exception.BadRequestException;
import ru.kontur.vostok.hercules.gate.client.exception.UnavailableClusterException;
import ru.kontur.vostok.hercules.util.concurrent.Topology;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author Daniil Zhenikhov
 */
public class GateClientTests {
    private static final String OK_200_ADDR = "ok_2xx";
    private static final String ERROR_4XX_ADDR = "error_4xx";
    private static final String ERROR_5XX_ADDR = "error_5xx";
    private static final String ERROR_5XX_ADDR_PROCESSING_TEST = "error_5xx_processing_test";

    private Properties properties = new Properties();
    private final CloseableHttpClient HTTP_CLIENT = new CloseableHttpClientMock();
    private Topology<String> whiteList;
    private final String apiKey = "ordinary-api-key";

    @Before
    public void setUp() {
        whiteList = new Topology<>(new String[]{});
    }

    @Test(expected = BadRequestException.class)
    public void shouldThrowExceptionIfReturn4xx() throws BadRequestException, UnavailableClusterException {
        whiteList.add(ERROR_4XX_ADDR);
        GateClient gateClient = new GateClient(properties, HTTP_CLIENT, whiteList, apiKey);
        gateClient.ping();
    }

    @Test(expected = UnavailableClusterException.class)
    public void shouldThrowExceptionIfAllOneHostUnavailable() throws BadRequestException, UnavailableClusterException {
        whiteList.add(ERROR_5XX_ADDR);
        GateClient gateClient = new GateClient(properties, HTTP_CLIENT, whiteList, apiKey);
        gateClient.ping();
    }

    @Test(expected = UnavailableClusterException.class)
    public void shouldThrowExceptionIfAllHostUnavailable() throws BadRequestException, UnavailableClusterException {
        whiteList.add(ERROR_5XX_ADDR);
        whiteList.add(ERROR_5XX_ADDR);
        whiteList.add(ERROR_5XX_ADDR);
        GateClient gateClient = new GateClient(properties, HTTP_CLIENT, whiteList, apiKey);
        gateClient.ping();
    }

    @Test
    public void shouldDeleteUrlFromWhiteListAndReturnAfterTimeout() throws BadRequestException, UnavailableClusterException, InterruptedException {
        Properties properties = new Properties();
        properties.setProperty("greyListElementsRecoveryTimeMs", "100");
        whiteList.add(ERROR_5XX_ADDR);
        whiteList.add(ERROR_5XX_ADDR);
        whiteList.add(OK_200_ADDR);
        GateClient gateClient = new GateClient(properties, HTTP_CLIENT, whiteList, apiKey);
        gateClient.ping();
        assertEquals(1, whiteList.size());
        Thread.sleep(1000);
        assertEquals(3, whiteList.size());
    }

    @Test
    public void shouldNotPingNotWorkingHosts() throws BadRequestException, UnavailableClusterException {

        long startProcessingMs = System.currentTimeMillis();

        Properties properties = new Properties();
        properties.setProperty("greyListElementsRecoveryTimeMs", "10000");
        int count = 10;
        for (int i = 0; i < count; i++) {
            if (i % 2 == 0) {
                whiteList.add(OK_200_ADDR);
            } else {
                whiteList.add(ERROR_5XX_ADDR_PROCESSING_TEST);
            }
        }

        GateClient gateClient = new GateClient(properties, HTTP_CLIENT, whiteList, apiKey);
        for (int i = 0; i < count * 3; i++) {
            gateClient.ping();
        }

        long endProcessingMs = System.currentTimeMillis();
        long timeOfProcessingMs = endProcessingMs - startProcessingMs;

        assertTrue(timeOfProcessingMs > 5_000 && timeOfProcessingMs < 6_000);
    }

    /**
     * This test verifies that client method {@link GateClient#send} adds correct Authorization header with
     * given API key.
     *
     * @throws Exception Will be thrown if test incorrectly configure environment or if logic is incorrect.
     */
    @Test
    public void sendMethodShouldAddAuthorizationHeader() throws Exception {
        whiteList.add("some-host");
        CloseableHttpClient httpClient = mockAlwaysSuccessHttpClient();
        GateClient gateClient = new GateClient(properties, httpClient, whiteList, apiKey);
        String stream = "stream";
        byte[] data = "data".getBytes(StandardCharsets.UTF_8);

        gateClient.send(apiKey, stream, data);

        ArgumentCaptor<HttpUriRequest> requestCaptor = ArgumentCaptor.forClass(HttpUriRequest.class);
        verify(httpClient).execute(requestCaptor.capture());
        HttpUriRequest actualRequest = requestCaptor.getValue();
        Header[] authorization = actualRequest.getHeaders(HttpHeaders.AUTHORIZATION);
        assertEquals(1, authorization.length);
        assertEquals("Hercules apiKey ordinary-api-key", authorization[0].getValue());
    }

    private CloseableHttpClient mockAlwaysSuccessHttpClient() throws IOException {
        var httpClient = mock(CloseableHttpClient.class);
        var httpResponse = mock(CloseableHttpResponse.class);
        var httpResponseStatusLine = mock(StatusLine.class);
        doReturn(httpResponse).when(httpClient).execute(any(HttpUriRequest.class));
        doReturn(httpResponseStatusLine).when(httpResponse).getStatusLine();
        doReturn(200).when(httpResponseStatusLine).getStatusCode();
        return httpClient;
    }
}
