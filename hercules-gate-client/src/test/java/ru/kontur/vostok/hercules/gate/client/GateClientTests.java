package ru.kontur.vostok.hercules.gate.client;

import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Stubber;
import ru.kontur.vostok.hercules.gate.client.GateClient.Props;
import ru.kontur.vostok.hercules.gate.client.exception.BadRequestException;
import ru.kontur.vostok.hercules.gate.client.exception.UnavailableClusterException;
import ru.kontur.vostok.hercules.util.concurrent.Topology;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Daniil Zhenikhov
 */
@ExtendWith(MockitoExtension.class)
class GateClientTests {

    private static final String API_KEY = "ordinary-api-key";

    @Mock
    private CloseableHttpClient httpClient;

    static Stream<Arguments> errorsAndExceptions() {
        return Stream.of(
                Arguments.of(topologyOfElements("address"), 400, BadRequestException.class),
                Arguments.of(topologyOfElements("address"), 500, UnavailableClusterException.class),
                Arguments.of(topologyOfElements("address1", "address2"), 500, UnavailableClusterException.class)
        );
    }

    @ParameterizedTest
    @MethodSource("errorsAndExceptions")
    void shouldThrowExceptionOnHttpErrors(
            Topology<String> whiteList, int responseStatusCode, Class<? extends Throwable> expectedExceptionType
    ) throws Exception {
        doReturnStatusCode(responseStatusCode).when(httpClient).execute(any(HttpUriRequest.class));
        var gateClient = new GateClient(new Properties(), httpClient, whiteList, API_KEY);

        assertThrows(expectedExceptionType, gateClient::ping);
    }

    @Test
    void shouldDeleteUrlFromWhiteListAndReturnAfterTimeout() throws Exception {
        var properties = PropertiesUtil.ofEntries(Map.entry(Props.GREY_LIST_ELEMENTS_RECOVERY_TIME_MS.name(), "100"));
        var whiteList = topologyOfElements("address1", "address2", "address3");
        var executorService = mock(ScheduledExecutorService.class);
        var clock = mock(Clock.class);
        var runnableArgumentCaptor = ArgumentCaptor.forClass(Runnable.class);

        var gateClient = new GateClient(properties, httpClient, whiteList, API_KEY, executorService, clock);
        verify(executorService).scheduleWithFixedDelay(runnableArgumentCaptor.capture(), eq(100L), eq(100L), eq(TimeUnit.MILLISECONDS));
        Runnable updateTopologyTask = runnableArgumentCaptor.getValue();
        assertThat(updateTopologyTask, notNullValue());

        doReturnStatusCode(500, 500, 200).when(httpClient).execute(any());
        doReturn(100L, 200L).when(clock).millis();
        gateClient.ping();
        assertThat(whiteList.size(), is(1));
        verify(clock, times(2)).millis();
        reset(clock);

        doReturn(199L).when(clock).millis();
        updateTopologyTask.run();
        assertThat(whiteList.size(), is(1));
        verify(clock, atLeast(1)).millis();
        reset(clock);

        doReturn(200L).when(clock).millis();
        updateTopologyTask.run();
        assertThat(whiteList.size(), is(2));
        verify(clock, atLeast(1)).millis();
        reset(clock);

        doReturn(300L).when(clock).millis();
        updateTopologyTask.run();
        assertThat(whiteList.size(), is(3));
        verify(clock, atLeast(1)).millis();
        reset(clock);
    }

    @Test
    void shouldNotPingNotWorkingHosts() throws Exception {
        var whiteList = topologyOfElements("address1", "address2");
        var properties = PropertiesUtil.ofEntries(Map.entry(Props.GREY_LIST_ELEMENTS_RECOVERY_TIME_MS.name(), "100"));
        var executorService = mock(ScheduledExecutorService.class);
        var clock = mock(Clock.class);
        var gateClient = new GateClient(properties, httpClient, whiteList, API_KEY, executorService, clock);
        var httpRequestCaptor = ArgumentCaptor.forClass(HttpUriRequest.class);
        doReturnStatusCode(500, 200).when(httpClient).execute(any());
        gateClient.ping();
        assumeTrue(whiteList.size() == 1);
        reset(httpClient);
        var workingAddress = whiteList.next();

        doReturnStatusCode(200).when(httpClient).execute(any());
        gateClient.ping();
        gateClient.ping();
        verify(httpClient, times(2)).execute(httpRequestCaptor.capture());
        Set<String> uris = httpRequestCaptor.getAllValues().stream()
                .map(HttpUriRequest::getURI)
                .map(URI::toString)
                .collect(Collectors.toUnmodifiableSet());

        assertThat(uris.size(), is(1));
        assertThat(uris, hasItem(containsString(workingAddress)));
        assertThat(whiteList.size(), is(1));
        verify(httpClient, times(2)).execute(any(HttpUriRequest.class));
    }

    /**
     * This test verifies that client method {@link GateClient#send} adds correct Authorization header with
     * given API key.
     *
     * @throws Exception Will be thrown if test incorrectly configure environment or if logic is incorrect.
     */
    @Test
    void sendMethodShouldAddAuthorizationHeader() throws Exception {
        var whiteList = topologyOfElements("some-host");
        CloseableHttpClient httpClient = mockAlwaysSuccessHttpClient();
        GateClient gateClient = new GateClient(new Properties(), httpClient, whiteList, API_KEY);
        String stream = "stream";
        byte[] data = "data".getBytes(StandardCharsets.UTF_8);

        gateClient.send(API_KEY, stream, data);

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
    
    static Topology<String> topologyOfElements(String ...args) {
        return new Topology<>(args);
    }

    static Stubber doReturnStatusCode(Integer code, Integer ...otherCodes) {
        return doReturn(createResponseWithCode(code, otherCodes));
    }

    static CloseableHttpResponse createResponseWithCode(Integer code, Integer ...otherCodes) {
        var statusLine = Mockito.mock(StatusLine.class);
        when(statusLine.getStatusCode()).thenReturn(code, otherCodes);
        var response = mock(CloseableHttpResponse.class);
        doReturn(statusLine).when(response).getStatusLine();
        return response;
    }
}
