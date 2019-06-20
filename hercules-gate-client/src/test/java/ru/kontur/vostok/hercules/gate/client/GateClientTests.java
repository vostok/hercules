package ru.kontur.vostok.hercules.gate.client;

import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Before;
import org.junit.Test;
import ru.kontur.vostok.hercules.gate.client.exception.BadRequestException;
import ru.kontur.vostok.hercules.gate.client.exception.UnavailableClusterException;
import ru.kontur.vostok.hercules.util.concurrent.Topology;

import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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

    @Before
    public void setUp() {
        whiteList = new Topology<>(new String[]{});
    }

    @Test(expected = BadRequestException.class)
    public void shouldThrowExceptionIfReturn4xx() throws BadRequestException, UnavailableClusterException {
        whiteList.add(ERROR_4XX_ADDR);
        GateClient gateClient = new GateClient(properties, HTTP_CLIENT, whiteList);
        gateClient.ping();
    }

    @Test(expected = UnavailableClusterException.class)
    public void shouldThrowExceptionIfAllOneHostUnavailable() throws BadRequestException, UnavailableClusterException {
        whiteList.add(ERROR_5XX_ADDR);
        GateClient gateClient = new GateClient(properties, HTTP_CLIENT, whiteList);
        gateClient.ping();
    }

    @Test(expected = UnavailableClusterException.class)
    public void shouldThrowExceptionIfAllHostUnavailable() throws BadRequestException, UnavailableClusterException {
        whiteList.add(ERROR_5XX_ADDR);
        whiteList.add(ERROR_5XX_ADDR);
        whiteList.add(ERROR_5XX_ADDR);
        GateClient gateClient = new GateClient(properties, HTTP_CLIENT, whiteList);
        gateClient.ping();
    }

    @Test
    public void shouldDeleteUrlFromWhiteListAndReturnAfterTimeout() throws BadRequestException, UnavailableClusterException, InterruptedException {
        Properties properties = new Properties();
        properties.setProperty("greyListElementsRecoveryTimeMs", "100");
        whiteList.add(ERROR_5XX_ADDR);
        whiteList.add(ERROR_5XX_ADDR);
        whiteList.add(OK_200_ADDR);
        GateClient gateClient = new GateClient(properties, HTTP_CLIENT, whiteList);
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

        GateClient gateClient = new GateClient(properties, HTTP_CLIENT, whiteList);
        for (int i = 0; i < count * 3; i++) {
            gateClient.ping();
        }

        long endProcessingMs = System.currentTimeMillis();
        long timeOfProcessingMs = endProcessingMs - startProcessingMs;

        assertTrue(timeOfProcessingMs > 5_000 && timeOfProcessingMs < 6_000);
    }
}
