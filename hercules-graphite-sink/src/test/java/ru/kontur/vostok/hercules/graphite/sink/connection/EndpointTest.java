package ru.kontur.vostok.hercules.graphite.sink.connection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import ru.kontur.vostok.hercules.util.functional.Result;
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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;

/**
 * @author Gregory Koshelev
 */
@RunWith(MockitoJUnitRunner.class)
public class EndpointTest {
    @Mock
    private Socket socket;

    @Mock
    private OutputStream outputStream;

    @Mock
    private ExecutorService executorService;

    @Mock
    private Future<Result<Void, Exception>> future;

    @Test
    public void shouldCorrectlyLeaseConnectionIfFrozen() throws Exception {
        TimeSource time = new MockTimeSource();
        Endpoint endpoint = getEndpointWithMockExecutor(time, 1);

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
        Endpoint endpoint = getEndpointWithMockExecutor(time, 1);

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
        Mockito.when(executorService.submit(ArgumentMatchers.<Callable<Result<Void, Exception>>>any())).thenReturn(future);
        Mockito.when(future.get(anyLong(), any())).thenThrow(new TimeoutException());

        Endpoint endpoint = getEndpointWithMockExecutor(new MockTimeSource(), 1);

        try (Channel channel = endpoint.channel()) {
            channel.send(metricsList());
        } catch (EndpointException ex) {
            throw ex.getCause();
        }
    }

    @Test(expected = IOException.class)
    public void shouldGotIOException() throws Throwable {
        Mockito.when(socket.getOutputStream()).thenReturn(new ByteArrayOutputStream());
        Mockito.when(executorService.submit(ArgumentMatchers.<Callable<Result<Void, Exception>>>any())).thenReturn(future);
        Mockito.when(future.get(anyLong(), any())).thenReturn(Result.error(new IOException()));
        Endpoint endpoint = getEndpointWithMockExecutor(new MockTimeSource(), 10_000);

        try (Channel channel = endpoint.channel()) {
            channel.send(metricsList());
        } catch (EndpointException ex) {
            throw ex.getCause();
        }
    }

    @Test
    public void shouldCorrectlySend() throws Throwable {
        Mockito.when(socket.getOutputStream()).thenReturn(new ByteArrayOutputStream());
        Mockito.when(executorService.submit(ArgumentMatchers.<Callable<Result<Void, Exception>>>any())).thenReturn(future);
        Mockito.when(future.get(anyLong(), any())).thenReturn(Result.ok());
        Endpoint endpoint = getEndpointWithMockExecutor(new MockTimeSource(), 10_000);

        try (Channel channel = endpoint.channel()) {
            channel.send(metricsList());
        }
    }

    @Test
    public void shouldCorrectlyClose() throws Throwable {
        Mockito.when(socket.getOutputStream()).thenReturn(new ByteArrayOutputStream());
        Mockito.when(executorService.submit(ArgumentMatchers.<Callable<Result<Void, Exception>>>any())).thenReturn(future);
        Mockito.when(future.get(anyLong(), any())).thenReturn(Result.ok());
        Endpoint endpoint = getEndpointWithMockExecutor(new MockTimeSource(), 10_000);

        try (Channel channel = endpoint.channel()) {
            channel.send(metricsList());
        }
        assertFalse(endpoint.connections.isEmpty());

        endpoint.close();
        assertTrue(endpoint.connections.isEmpty());
        Mockito.verify(executorService).shutdown();
    }

    @Test
    public void shouldMaintainConnections() throws Throwable {
        Mockito.when(socket.getOutputStream()).thenReturn(new ByteArrayOutputStream());

        Endpoint endpoint = getEndpointWithMockExecutor(new MockTimeSource(), 10_000);

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

        Endpoint endpoint = getEndpointWithMockExecutor(new MockTimeSource(), 10_000);

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

        Endpoint endpoint = getEndpointWithMockExecutor(new MockTimeSource(), 10_000);

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
        Endpoint endpoint = getEndpointWithMockExecutor(new MockTimeSource(), 10_000);

        try (Channel channel = endpoint.channel()) {
            assertNotNull(channel);
            assertEquals(0, endpoint.connections.size());
        }

        assertEquals(2, endpoint.connections.size());
    }

    @Test
    public void shouldMaintainOneConnections_channelForRetry() throws Throwable {
        Mockito.when(socket.getOutputStream()).thenReturn(new ByteArrayOutputStream());
        Endpoint endpoint = getEndpointWithMockExecutor(new MockTimeSource(), 10_000);

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
        Endpoint endpoint = getEndpoint();

        Mockito.when(socket.getOutputStream()).thenReturn(outputStream);
        Mockito.doThrow(new IOException()).when(outputStream).flush();

        // add broken connection on pool
        try (Channel channel = endpoint.channel()) {
            assertNotNull(channel);
        }

        Mockito.when(socket.getOutputStream()).thenReturn(new ByteArrayOutputStream());

        return endpoint;
    }

    private static List<String> metricsList() {
        Random random = new Random();
        List<String> data = new ArrayList<>();
        data.add("name." + random.nextInt());
        return data;
    }

    private Endpoint getEndpoint() {
        return new Endpoint(
                InetSocketAddressUtil.fromString("127.0.0.1", 2003),
                2,
                CONNECTION_LIMIT,
                Long.MAX_VALUE,
                2_000,
                10_000,
                new MockTimeSource()) {
            @Override
            Socket getSocket() {
                return socket;
            }
        };
    }

    private Endpoint getEndpointWithMockExecutor(TimeSource time, int requestTimeoutMs) {
        return new Endpoint(
                executorService,
                InetSocketAddressUtil.fromString("127.0.0.1", 2003),
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

    private final int CONNECTION_LIMIT = 4;
}
