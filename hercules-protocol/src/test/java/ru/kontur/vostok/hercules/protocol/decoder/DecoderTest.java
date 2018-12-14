package ru.kontur.vostok.hercules.protocol.decoder;

import org.junit.Test;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.Type;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.protocol.decoder.exceptions.InvalidDataException;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

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
    public void testSingleEventBatchDecoding() throws IOException, URISyntaxException, InvalidDataException {
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
        assertEquals(15276799200000000L, event.getTimestamp());
        assertEquals(UUID.fromString("11203800-63FD-11E8-83E2-3A587D902000"), event.getRandom());

        assertEquals(2, event.getPayload().size());
        assertNotNull(event.getPayload().get("host"));
        assertNotNull(event.getPayload().get("timestamp"));

        Variant hostTagValue = event.getPayload().get("host");
        assertNotNull(hostTagValue);
        assertEquals(Type.STRING, hostTagValue.getType());
        Object host = hostTagValue.getValue();
        assertNotNull(host);
        assertTrue(host instanceof byte[]);
        byte[] hostAsBytes = (byte[]) host;
        assertEquals(9, hostAsBytes.length);
        assertArrayEquals("localhost".getBytes(StandardCharsets.UTF_8), hostAsBytes);

        Variant timestampTagValue = event.getPayload().get("timestamp");
        assertNotNull(timestampTagValue);
        assertEquals(Type.LONG, timestampTagValue.getType());
        Object timestamp = timestampTagValue.getValue();
        assertNotNull(timestamp);
        assertTrue(timestamp instanceof Long);
        long timestampAsLong = (Long) timestamp;
        assertEquals(1527679920000000L, timestampAsLong);

        byte[] bytes = event.getBytes();
        assertNotNull(bytes);
        assertEquals(65, bytes.length);
        assertArrayEquals(new byte[]{
                /* Version equals 1 */
                0x01,
                /* Timestamp equals 1527679920000000L */
                0x00, 0x36, 0x46, 0x2A, (byte) 0xFD, (byte) 0x9E, (byte) 0xF8, 0x00,
                /* Random is UUID for Timestamp 137469727200000000L is 2018-05-30T11:32:00.000Z in 100ns ticks from Gregorian Epoch */
                0x11, 0x20, 0x38, 0x00, 0x63, (byte) 0xFD, 0x11, (byte) 0xE8, (byte) 0x83, (byte) 0xE2, 0x3A, 0x58,
                0x7D, (byte) 0x90, 0x20, 0x00,
                /* Tags count equals 2 */
                0x00, 0x02,
                /* Tag's key 'host' */
                0x04, 0x68, 0x6F, 0x73, 0x74,
                /* Tag's value type String */
                0x09,
                /* Tag's value 'localhost' */
                0x00, 0x00, 0x00, 0x09, 0x6C, 0x6F, 0x63, 0x61, 0x6C, 0x68, 0x6F, 0x73, 0x74,
                /* Tag's key 'timestamp' */
                0x09, 0x74, 0x69, 0x6D, 0x65, 0x73, 0x74, 0x61, 0x6D, 0x70,
                /* Tag's value type Long */
                0x05,
                /*Tag's value 1527679920000000L */
                0x00, 0x05, 0x6D, 0x6A, (byte) 0xB2, (byte) 0xF6, 0x4C, 0x00
        }, bytes);
    }
}
