package ru.kontur.vostok.hercules.client.timeline.api;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Test;
import ru.kontur.vostok.hercules.client.LogicalShardState;
import ru.kontur.vostok.hercules.client.test.util.TestUtil;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.TimelineContent;
import ru.kontur.vostok.hercules.protocol.TimelineState;
import ru.kontur.vostok.hercules.protocol.TimelineSliceState;
import ru.kontur.vostok.hercules.protocol.encoder.TimelineContentWriter;
import ru.kontur.vostok.hercules.protocol.util.EventBuilder;
import ru.kontur.vostok.hercules.protocol.util.EventUtil;

import java.net.URI;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.kontur.vostok.hercules.client.test.util.TestUtil._200_OK;

public class TimelineApiClientTest {

    @Test
    public void getTimelineContent() throws Exception {

        CloseableHttpResponse responseMock = mock(CloseableHttpResponse.class);
        when(responseMock.getStatusLine()).thenReturn(TestUtil._200_OK);


        when(responseMock.getEntity()).thenReturn(new ByteArrayEntity(TestUtil.toBytes(
                new TimelineContent(
                        new TimelineState(
                                new TimelineSliceState[]{
                                        new TimelineSliceState(0, 123456789, EventUtil.eventIdAsBytes(137_620_098_108_949_610L, UUID.fromString("05bd046a-ecc0-11e8-8eb2-f2801f1b9fd1")))
                                }),
                        new Event[]{
                                EventBuilder.create(0,"05bd046a-ecc0-11e8-8eb2-f2801f1b9fd1").build(),
                                EventBuilder.create(0, "0b9e32b4-ecc0-11e8-8eb2-f2801f1b9fd1").build()
                        }),
                new TimelineContentWriter()
        )));

        CloseableHttpClient clientMock = mock(CloseableHttpClient.class);
        when(clientMock.execute(any(HttpUriRequest.class))).thenReturn(responseMock);

        TimelineApiClient client = new TimelineApiClient(
                () -> clientMock,
                URI.create("http://test/"),
                new LogicalShardState(0, 1),
                "test"
        );

        TimelineContent content = client.getTimelineContent(
                "test_tl_0",
                new TimelineState(new TimelineSliceState[]{}),
                new TimeInterval(1542758400000L, 1542759400000L),
                100
        );

        assertEquals(2, content.getEvents().length);

        assertEquals(UUID.fromString("05bd046a-ecc0-11e8-8eb2-f2801f1b9fd1"), content.getEvents()[0].getUuid());
        assertEquals(UUID.fromString("0b9e32b4-ecc0-11e8-8eb2-f2801f1b9fd1"), content.getEvents()[1].getUuid());
    }

    @Test
    public void ping() throws Exception {

        CloseableHttpResponse responseMock = mock(CloseableHttpResponse.class);
        when(responseMock.getStatusLine()).thenReturn(_200_OK);

        CloseableHttpClient clientMock = mock(CloseableHttpClient.class);
        when(clientMock.execute(any(HttpUriRequest.class))).thenReturn(responseMock);


        TimelineApiClient client = new TimelineApiClient(
                () -> clientMock,
                URI.create("http://test/"),
                new LogicalShardState(0, 1),
                "test"
        );

        assertTrue(client.ping());
    }
}
