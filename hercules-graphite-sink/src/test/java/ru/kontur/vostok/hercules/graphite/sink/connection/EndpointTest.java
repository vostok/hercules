package ru.kontur.vostok.hercules.graphite.sink.connection;

import org.junit.Test;
import ru.kontur.vostok.hercules.util.net.InetSocketAddressUtil;
import ru.kontur.vostok.hercules.util.time.MockTimeSource;
import ru.kontur.vostok.hercules.util.time.TimeSource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * @author Gregory Koshelev
 */
public class EndpointTest {
    @Test
    public void shouldCorrectlyLeaseConnectionIfFrozen() throws EndpointException {
        TimeSource time = new MockTimeSource();
        Endpoint endpoint =
                new Endpoint(
                        InetSocketAddressUtil.fromString("127.0.0.1", 2003),
                        3,
                        3_600_000,
                        2_000,
                        time);

        assertEquals(0, endpoint.leasedConnections());

        endpoint.freeze(10_000);

        assertNull(endpoint.channel());
        assertEquals(0, endpoint.leasedConnections());

        time.sleep(1_000);

        assertNull(endpoint.channel());
        assertEquals(0, endpoint.leasedConnections());
    }
}
