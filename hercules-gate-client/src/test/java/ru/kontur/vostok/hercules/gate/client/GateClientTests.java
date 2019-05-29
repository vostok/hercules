package ru.kontur.vostok.hercules.gate.client;

import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Before;
import org.junit.Test;
import ru.kontur.vostok.hercules.gate.client.exception.BadRequestException;
import ru.kontur.vostok.hercules.gate.client.exception.UnavailableClusterException;
import ru.kontur.vostok.hercules.util.concurrent.Topology;

import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.internal.verification.VerificationModeFactory.times;

/**
 * @author Daniil Zhenikhov
 */
public class GateClientTests {
    private static final String OK_200_ADDR = "ok_2xx";
    private static final String ERROR_4XX_ADDR = "error_4xx";
    private static final String ERROR_5XX_ADDR = "error_5xx";

    private Properties properties = new Properties();
    private final CloseableHttpClient HTTP_CLIENT = new CloseableHttpClientMock();
    private BlockingQueue<GreyListTopologyElement> greyList;
    private Topology<String> whiteList;

    @Before
    public void setUp() {
        greyList = new ArrayBlockingQueue<>(4);
        whiteList = new Topology<>(new String[]{});
    }

    @Test(expected = BadRequestException.class)
    public void shouldThrowExceptionIfReturn4xx() throws BadRequestException, UnavailableClusterException {
        whiteList.add(ERROR_4XX_ADDR);
        GateClient gateClient = new GateClient(properties, HTTP_CLIENT, whiteList, greyList);
        gateClient.ping();
    }

    @Test(expected = UnavailableClusterException.class)
    public void shouldThrowExceptionIfAllOneHostUnavailable() throws BadRequestException, UnavailableClusterException {
        whiteList.add(ERROR_5XX_ADDR);
        GateClient gateClient = new GateClient(properties, HTTP_CLIENT, whiteList, greyList);
        gateClient.ping();
    }

    @Test(expected = UnavailableClusterException.class)
    public void shouldThrowExceptionIfAllHostUnavailable() throws BadRequestException, UnavailableClusterException {
        whiteList.add(ERROR_5XX_ADDR);
        whiteList.add(ERROR_5XX_ADDR);
        whiteList.add(ERROR_5XX_ADDR);
        GateClient gateClient = new GateClient(properties, HTTP_CLIENT, whiteList, greyList);
        gateClient.ping();
    }

    @Test
    public void shouldPutIntoGreyListThreeTimes() throws BadRequestException, UnavailableClusterException, InterruptedException {
        greyList = mock(ArrayBlockingQueue.class);
        whiteList.add(ERROR_5XX_ADDR);
        whiteList.add(ERROR_5XX_ADDR);
        whiteList.add(ERROR_5XX_ADDR);
        whiteList.add(OK_200_ADDR);
        GateClient gateClient = new GateClient(properties, HTTP_CLIENT, whiteList, greyList);
        gateClient.ping();
        verify(greyList, times(3)).put(any());
        gateClient.ping();
        verify(greyList, times(3)).put(any());

    }

    @Test
    public void shouldNotPutIntoGreyList() throws BadRequestException, UnavailableClusterException, InterruptedException {
        greyList = mock(ArrayBlockingQueue.class);
        whiteList.add(OK_200_ADDR);
        whiteList.add(ERROR_5XX_ADDR);
        whiteList.add(ERROR_5XX_ADDR);
        whiteList.add(ERROR_5XX_ADDR);
        GateClient gateClient = new GateClient(properties, HTTP_CLIENT, whiteList, greyList);
        gateClient.ping();
        verify(greyList, times(0)).put(any());
    }
}
