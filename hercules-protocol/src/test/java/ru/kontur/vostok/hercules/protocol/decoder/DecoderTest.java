package ru.kontur.vostok.hercules.protocol.decoder;

import org.junit.Test;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.Type;
import ru.kontur.vostok.hercules.protocol.Variant;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Gregory Koshelev
 */
public class DecoderTest {
    @Test
    public void testSingleEventBatchDecoding() throws IOException, URISyntaxException {
        URL resource = getClass().getClassLoader().getResource("v1.event.1.txt");
        Path path = Paths.get(resource.toURI());
        byte[] data = Files.readAllBytes(path);
        Set<String> tags = new HashSet<>();
        tags.addAll(Arrays.asList("host", "timestamp"));

        ReaderIterator<Event> reader = new ReaderIterator<>(new Decoder(data), EventReader.readTags(tags));
        assertEquals(1, reader.getTotal());
        assertTrue(reader.hasNext());

        Event event = reader.next();
        assertNotNull(event);
        assertFalse(reader.hasNext());

        assertEquals(1, event.getVersion());
        assertEquals(137469727200000000L, event.getTimestamp());

        Map<String, Variant> tagValues = event.getTags();
        assertNotNull(tagValues);
        assertEquals(2, tagValues.size());
        assertTrue(tagValues.containsKey("host"));
        assertTrue(tagValues.containsKey("timestamp"));

        Variant hostTagValue = tagValues.get("host");
        assertNotNull(hostTagValue);
        assertEquals(Type.STRING, hostTagValue.getType());
        Object host = hostTagValue.getValue();
        assertNotNull(host);
        assertTrue(host instanceof byte[]);
        byte[] hostAsBytes = (byte[]) host;
        assertEquals(9, hostAsBytes.length);
        assertArrayEquals("localhost".getBytes(StandardCharsets.UTF_8), hostAsBytes);

        Variant timestampTagValue = tagValues.get("timestamp");
        assertNotNull(timestampTagValue);
        assertEquals(Type.LONG, timestampTagValue.getType());
        Object timestamp = timestampTagValue.getValue();
        assertNotNull(timestamp);
        assertTrue(timestamp instanceof Long);
        long timestampAsLong = (Long) timestamp;
        assertEquals(1527679920000000L, timestampAsLong);

        byte[] bytes = event.getBytes();
        assertNotNull(bytes);
        assertEquals(46, bytes.length);
        assertArrayEquals(new byte[]{
                0x01,/* Version */
                0x01, (byte) 0xE8, 0x63, (byte) 0xFD, 0x11, 0x20, 0x38, 0x00,/* Timestamp 137469727200000000L is 2018-05-30T11:32:00.000Z in 100ns ticks from Gregorian Epoch */
                0x00, 0x02,/* Tags count */
                0x04, 0x68, 0x6F, 0x73, 0x74,/* Tag name 'host' */
                0x08, 0x09, 0x6C, 0x6F, 0x63, 0x61, 0x6C, 0x68, 0x6F, 0x73, 0x74,/* Tag value 'localhost' of type String*/
                0x09, 0x74, 0x69, 0x6D, 0x65, 0x73, 0x74, 0x61, 0x6D, 0x70,/* Tag name 'timestamp' */
                0x04, 0x00, 0x05, 0x6D, 0x6A, (byte) 0xB2, (byte) 0xF6, 0x4C, 0x00 /*Tag value 1527679920000000L of type Long */
        }, bytes);
    }
}
