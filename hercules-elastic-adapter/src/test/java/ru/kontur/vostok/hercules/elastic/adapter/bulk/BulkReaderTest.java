package ru.kontur.vostok.hercules.elastic.adapter.bulk;

import org.junit.Test;
import ru.kontur.vostok.hercules.elastic.adapter.bulk.action.IndexAction;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Gregory Koshelev
 */
public class BulkReaderTest {
    @Test
    public void shouldReadBulkWithSingleDocument() {
        byte[] data = ("{\"index\": {\"_index\": \"test_index\", \"_type\": \"LogEvent\"}}\n" +
                "{\"@timestamp\": \"2020-04-07T20:22:00.000Z\", \"level\": \"INFO\", \"message\": \"Test message\"}").
                getBytes(StandardCharsets.UTF_8);

        Iterator<IndexRequest> iterator = BulkReader.read(data, null, null);
        assertTrue(iterator.hasNext());

        IndexRequest indexRequest = iterator.next();

        IndexAction action = indexRequest.getAction();
        assertNotNull(action);
        assertEquals("LogEvent", action.getType());
        assertEquals("test_index", action.getIndex());
        assertNull(action.getId());


        Map<String, Object> document = indexRequest.getDocument();
        assertNotNull(document);

        Map<String, Object> expected = new HashMap<>();
        expected.put("@timestamp", "2020-04-07T20:22:00.000Z");
        expected.put("level", "INFO");
        expected.put("message", "Test message");
        assertMapEquals(expected, document);
    }

    @Test
    public void shouldReadBulkWithMultipleDocuments() {
        byte[] data = ("{\"index\": {\"_index\": \"test_index\", \"_type\": \"LogEvent\"}}\n" +
                "{\"@timestamp\": \"2020-04-07T21:08:00.000Z\", \"level\": \"INFO\", \"message\": \"First message\"}\n" +
                "{\"index\": {\"_index\": \"test_index\", \"_type\": \"LogEvent\"}}\n" +
                "{\"@timestamp\": \"2020-04-07T21:08:00.000Z\", \"level\": \"DEBUG\", \"message\": \"Second message\"}\n").
                getBytes(StandardCharsets.UTF_8);

        Iterator<IndexRequest> iterator = BulkReader.read(data, null, null);

        assertTrue(iterator.hasNext());
        assertEquals("First message", iterator.next().getDocument().get("message"));

        assertTrue(iterator.hasNext());
        assertEquals("Second message", iterator.next().getDocument().get("message"));

        assertFalse(iterator.hasNext());
    }

    @Test
    public void shouldIgnoreTrailingNonIndexAction() {
        byte[] data = ("{ \"index\" : { \"_index\" : \"test\", \"_id\" : \"1\" } }\n" +
                "{ \"field1\" : \"value1\" }\n" +
                "{ \"delete\" : { \"_index\" : \"test\", \"_id\" : \"2\" } }").
                getBytes(StandardCharsets.UTF_8);

        Iterator<IndexRequest> iterator = BulkReader.read(data, null, null);

        assertTrue(iterator.hasNext());
        IndexRequest indexRequest = iterator.next();

        assertEquals("1", indexRequest.getAction().getId());
        assertMapEquals(Collections.singletonMap("field1", "value1"), indexRequest.getDocument());

        assertFalse(iterator.hasNext());
    }

    @Test
    public void shouldIgnoreLeadingNonIndexAction() {
        byte[] data = ("{ \"delete\" : { \"_index\" : \"test\", \"_id\" : \"2\" } }\n" +
                "{ \"index\" : { \"_index\" : \"test\", \"_id\" : \"1\" } }\n" +
                "{ \"field1\" : \"value1\" }\n").
                getBytes(StandardCharsets.UTF_8);

        Iterator<IndexRequest> iterator = BulkReader.read(data, null, null);

        assertTrue(iterator.hasNext());
        IndexRequest indexRequest = iterator.next();

        assertEquals("1", indexRequest.getAction().getId());
        assertMapEquals(Collections.singletonMap("field1", "value1"), indexRequest.getDocument());

        assertFalse(iterator.hasNext());
    }

    @Test
    public void shouldUseDefaultIndexAndType() {
        byte[] data = ("{\"index\": {}}\n" +
                "{\"@timestamp\": \"2020-04-07T20:22:00.000Z\", \"level\": \"INFO\", \"message\": \"Test message\"}").
                getBytes(StandardCharsets.UTF_8);

        Iterator<IndexRequest> iterator = BulkReader.read(data, "test_index", "LogEvent");
        assertTrue(iterator.hasNext());

        IndexRequest indexRequest = iterator.next();

        IndexAction action = indexRequest.getAction();
        assertNotNull(action);
        assertEquals("LogEvent", action.getType());
        assertEquals("test_index", action.getIndex());
    }

    private <K, V> void assertMapEquals(Map<K, V> expected, Map<K, V> actual) {
        assertEquals(expected.size(), actual.size());
        for (Map.Entry<K, V> entry : expected.entrySet()) {
            K key = entry.getKey();
            V value = entry.getValue();

            assertTrue(actual.containsKey(key));
            assertEquals(value, actual.get(key));
        }
    }
}
