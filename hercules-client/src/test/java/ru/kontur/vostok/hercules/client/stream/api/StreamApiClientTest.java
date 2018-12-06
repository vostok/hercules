package ru.kontur.vostok.hercules.client.stream.api;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Test;
import ru.kontur.vostok.hercules.client.LogicalShardState;
import ru.kontur.vostok.hercules.client.test.util.TestUtil;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.EventStreamContent;
import ru.kontur.vostok.hercules.protocol.StreamReadState;
import ru.kontur.vostok.hercules.protocol.StreamShardReadState;
import ru.kontur.vostok.hercules.protocol.encoder.EventStreamContentWriter;
import ru.kontur.vostok.hercules.protocol.util.EventBuilder;

import java.net.URI;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class StreamApiClientTest {

    @Test
    public void ping() throws Exception {
        CloseableHttpResponse responseMock = mock(CloseableHttpResponse.class);
        when(responseMock.getStatusLine()).thenReturn(TestUtil._200_OK);

        CloseableHttpClient clientMock = mock(CloseableHttpClient.class);
        when(clientMock.execute(any(HttpUriRequest.class))).thenReturn(responseMock);

        StreamApiClient client = new StreamApiClient(
                () -> clientMock,
                URI.create("http://test/"),
                new LogicalShardState(0, 1),
                "test"
        );

        assertTrue(client.ping());
    }

    @Test
    public void read() throws Exception {
        CloseableHttpResponse responseMock = mock(CloseableHttpResponse.class);
        when(responseMock.getStatusLine()).thenReturn(TestUtil._200_OK);


        when(responseMock.getEntity()).thenReturn(new ByteArrayEntity(TestUtil.toBytes(
                new EventStreamContent(
                        new StreamReadState(
                                new StreamShardReadState[]{
                                        new StreamShardReadState(0, 2)
                                }),
                        new Event[]{
                                new EventBuilder().setRandom(UUID.fromString("05bd046a-ecc0-11e8-8eb2-f2801f1b9fd1")).build(),
                                new EventBuilder().setRandom(UUID.fromString("0b9e32b4-ecc0-11e8-8eb2-f2801f1b9fd1")).build()
                        }),
                new EventStreamContentWriter()
        )));

        CloseableHttpClient clientMock = mock(CloseableHttpClient.class);
        when(clientMock.execute(any(HttpUriRequest.class))).thenReturn(responseMock);

        StreamApiClient client = new StreamApiClient(
                () -> clientMock,
                URI.create("http://test/"),
                new LogicalShardState(0, 1),
                "test"
        );

        EventStreamContent content = client.getStreamContent(
                "test",
                new StreamReadState(new StreamShardReadState[]{}),
                2
        );

        assertEquals(2, content.getEvents().length);

        assertEquals(UUID.fromString("05bd046a-ecc0-11e8-8eb2-f2801f1b9fd1"), content.getEvents()[0].getRandom());
        assertEquals(UUID.fromString("0b9e32b4-ecc0-11e8-8eb2-f2801f1b9fd1"), content.getEvents()[1].getRandom());
    }
}
