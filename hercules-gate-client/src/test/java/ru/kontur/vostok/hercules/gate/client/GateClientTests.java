package ru.kontur.vostok.hercules.gate.client;

import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Test;
import ru.kontur.vostok.hercules.gate.client.exception.BadRequestException;
import ru.kontur.vostok.hercules.gate.client.exception.HttpProtocolException;
import ru.kontur.vostok.hercules.gate.client.exception.UnavailableClusterException;
import ru.kontur.vostok.hercules.gate.client.exception.UnavailableHostException;

/**
 * @author Daniil Zhenikhov
 */
public class GateClientTests {
    private static final String ERROR_4XX_ADDR = "error_client_1";
    private static final String ERROR_5XX_ADDR = "error_host_1";
    private static final String ERROR_503_ADDR = "error_host_2";
    private static final String CLIENT_PROTOCOL_EXC_ADDR = "error_client_2";
    private static final String IOEXC_ADDR = "error_host_3";

    private static final CloseableHttpClient HTTP_CLIENT = new CloseableHttpClientMock();
    private static final GateClient GATE_CLIENT = new GateClient(HTTP_CLIENT);

    @Test(expected = BadRequestException.class)
    public void shouldThrow_Host_Return4xx() throws BadRequestException, UnavailableHostException, HttpProtocolException {
        GATE_CLIENT.ping(ERROR_4XX_ADDR);
    }

    @Test(expected = UnavailableHostException.class)
    public void shouldThrow_Hots_return5xx() throws BadRequestException, UnavailableHostException, HttpProtocolException {
        GATE_CLIENT.ping(ERROR_5XX_ADDR);
    }

    @Test(expected = BadRequestException.class)
    public void shouldThrow_Cluster_return4xx() throws BadRequestException, UnavailableClusterException {
        GATE_CLIENT.ping(new String[]{ERROR_4XX_ADDR, ERROR_5XX_ADDR, ERROR_503_ADDR});
    }

    @Test(expected = UnavailableClusterException.class)
    public void shouldThrow_Cluster_return5xx() throws BadRequestException, UnavailableClusterException {
        GATE_CLIENT.ping(new String[]{ERROR_5XX_ADDR, ERROR_503_ADDR});
    }

    @Test(expected = HttpProtocolException.class)
    public void shouldThrow_Host_throwClientProtocolExc() throws BadRequestException, UnavailableHostException, HttpProtocolException {
        GATE_CLIENT.ping(CLIENT_PROTOCOL_EXC_ADDR);
    }

    @Test(expected = UnavailableHostException.class)
    public void shouldThrow_Host_throwIOException() throws BadRequestException, UnavailableHostException, HttpProtocolException {
        GATE_CLIENT.ping(IOEXC_ADDR);
    }
}
