package ru.kontur.vostok.hercules.graphite.sink.connection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import ru.kontur.vostok.hercules.graphite.sink.GraphiteMetricData;
import ru.kontur.vostok.hercules.util.net.InetSocketAddressUtil;
import ru.kontur.vostok.hercules.util.time.MockTimeSource;
import ru.kontur.vostok.hercules.util.time.TimeSource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeoutException;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Gregory Koshelev
 */
@RunWith(MockitoJUnitRunner.class)
public class EndpointTest {
    @Mock
    private Socket socket;

    @Mock
    private OutputStream outputStream;

    @Test
    public void shouldCorrectlyLeaseConnectionIfFrozen() throws Exception {
        TimeSource time = new MockTimeSource();
        Endpoint endpoint = getEndpoint(time, 1);

        assertEquals(0, endpoint.connections.size());

        endpoint.freeze(10_000);

        assertNull(endpoint.channel());
        assertEquals(0, endpoint.connections.size());

        time.sleep(1_000);

        assertNull(endpoint.channelForRetry());
        assertEquals(0, endpoint.connections.size());
    }

    @Test
    public void shouldCorrectlyWorkAfterFreeze() throws Exception {
        Mockito.when(socket.getOutputStream()).thenReturn(new ByteArrayOutputStream());
        TimeSource time = new MockTimeSource();
        Endpoint endpoint = getEndpoint(time, 1);

        assertEquals(0, endpoint.connections.size());

        endpoint.freeze(10_000);

        assertNull(endpoint.channel());
        assertEquals(0, endpoint.connections.size());

        time.sleep(11_000);

        try (Channel channel = endpoint.channel()) {
            assertNotNull(channel);
        }
        assertEquals(2, endpoint.connections.size());
    }

    @Test(expected = TimeoutException.class)
    public void shouldGetTimeout() throws Throwable {
        Mockito.when(socket.getOutputStream()).thenReturn(new ByteArrayOutputStream());

        Endpoint endpoint = getEndpoint(new MockTimeSource(), 1);

        Random random = new Random();
        ArrayList<GraphiteMetricData> data = new ArrayList<>();
        for (int i = 0; i < 10000; i++) {
            data.add(new GraphiteMetricData("name", random.nextLong(), random.nextDouble()));
        }
        try (Channel channel = endpoint.channel()) {
            channel.send(data);
        } catch (EndpointException ex) {
            throw ex.getCause();
        }
    }

    @Test
    public void shouldCorrectlySend() throws Throwable {
        Mockito.when(socket.getOutputStream()).thenReturn(new ByteArrayOutputStream());

        Endpoint endpoint = getEndpoint(new MockTimeSource(), 10_000);

        List<GraphiteMetricData> data = metricsList();
        try (Channel channel = endpoint.channel()) {
            channel.send(data);
        }
    }

    @Test
    public void shouldMaintainConnections() throws Throwable {
        Mockito.when(socket.getOutputStream()).thenReturn(new ByteArrayOutputStream());

        Endpoint endpoint = getEndpoint(new MockTimeSource(), 10_000);

        for (int i = 0; i < CONNECTION_LIMIT; i++) {
            try (Channel channel = endpoint.channel()) {
                assertNotNull(channel);
            }
        }

        assertEquals(CONNECTION_LIMIT, endpoint.connections.size());
    }

    @Test
    public void shouldNotCreateMoreThanLimitPlusThreadCount() throws Throwable {
        Mockito.when(socket.getOutputStream()).thenReturn(new ByteArrayOutputStream());

        Endpoint endpoint = getEndpoint(new MockTimeSource(), 10_000);

        for (int i = 0; i < CONNECTION_LIMIT * 10; i++) {
            try (Channel channel = endpoint.channel()) {
                assertNotNull(channel);
            }
        }

        assertTrue(endpoint.connections.size() <= CONNECTION_LIMIT);
    }

    @Test
    public void shouldNotCreateMoreThanLimitPlusThreadCount_manyThreads() throws Throwable {
        Mockito.when(socket.getOutputStream()).thenReturn(new ByteArrayOutputStream());

        Endpoint endpoint = getEndpoint(new MockTimeSource(), 10_000);

        int parallelism = 3;
        ForkJoinPool customThreadPool = new ForkJoinPool(parallelism);
        customThreadPool.submit(() ->
                IntStream.range(0, 1000).parallel().forEach(id -> {
                    try (Channel channel = endpoint.channel()) {
                        assertNotNull(channel);
                    } catch (EndpointException e) {
                        throw new RuntimeException(e);
                    }
                })
        ).get();
        customThreadPool.shutdown();

        assertTrue(endpoint.connections.size() <= CONNECTION_LIMIT + parallelism - 1);
    }

    @Test
    public void shouldMaintainOneConnections_channel() throws Throwable {
        Mockito.when(socket.getOutputStream()).thenReturn(new ByteArrayOutputStream());
        Endpoint endpoint = getEndpoint(new MockTimeSource(), 10_000);

        try (Channel channel = endpoint.channel()) {
            assertNotNull(channel);
            assertEquals(0, endpoint.connections.size());
        }

        assertEquals(2, endpoint.connections.size());
    }

    @Test
    public void shouldMaintainOneConnections_channelForRetry() throws Throwable {
        Mockito.when(socket.getOutputStream()).thenReturn(new ByteArrayOutputStream());
        Endpoint endpoint = getEndpoint(new MockTimeSource(), 10_000);

        try (Channel channel = endpoint.channelForRetry()) {
            assertNotNull(channel);
            assertEquals(0, endpoint.connections.size());
        }

        assertEquals(2, endpoint.connections.size());
    }

    @Test
    public void shouldCreateNewConnectionOnRetry() throws Throwable {
        Endpoint endpoint = sendRetryTest();
        try (Channel channel = endpoint.channelForRetry()) {
            channel.send(metricsList());
        }
    }

    @Test(expected = EndpointException.class)
    public void reuseConnectionAndFail() throws Throwable {
        Endpoint endpoint = sendRetryTest();
        try (Channel channel = endpoint.channel()) {
            channel.send(metricsList());
        }
    }

    private Endpoint sendRetryTest() throws IOException, EndpointException {
        TimeSource time = new MockTimeSource();
        Endpoint endpoint = getEndpoint(time, 10_000);


        Mockito.when(socket.getOutputStream()).thenReturn(outputStream);
        Mockito.doThrow(new IOException()).when(outputStream).flush();

        // add broken connection on pool
        try (Channel channel = endpoint.channel()) {
            assertNotNull(channel);
        }

        Mockito.when(socket.getOutputStream()).thenReturn(new ByteArrayOutputStream());

        return endpoint;
    }

    private static List<GraphiteMetricData> metricsList() {
        Random random = new Random();
        ArrayList<GraphiteMetricData> data = new ArrayList<>();
        data.add(new GraphiteMetricData("name", random.nextLong(), random.nextDouble()));
        return data;
    }

    private Endpoint getEndpoint(TimeSource time, int requestTimeoutMs) {
        return new Endpoint(
                InetSocketAddressUtil.fromString("127.0.0.1", 2003),
                2,
                CONNECTION_LIMIT,
                Long.MAX_VALUE,
                2_000,
                requestTimeoutMs,
                time) {
            @Override
            Socket getSocket() {
                return socket;
            }
        };
    }

    private int CONNECTION_LIMIT = 4;
}
