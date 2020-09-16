package ru.kontur.vostok.hercules.json;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Gregory Koshelev
 */
public class DocumentReaderTest {
    @Test
    public void shouldReadComplexDocument() {
        byte[] data = ("{\"@timestamp\": \"2020-04-07T20:22:00.000Z\", \"level\": \"INFO\", \"message\": \"Test message\", " +
                "\"map\": {\"field1\": \"value1\", \"field2\": \"value2\"}, " +
                "\"listOfIntegers\": [1, 2, 3], " +
                "\"listOfStrings\": [\"a\", \"b\", \"c\"]}").
                getBytes(StandardCharsets.UTF_8);
        Map<String, Object> document = DocumentReader.read(data).document();
        assertNotNull(document);

        Map<String, Object> map = (Map<String, Object>) document.get("map");
        assertNotNull(map);
        assertEquals(2, map.size());
        assertEquals("value1", map.get("field1"));
        assertEquals("value2", map.get("field2"));

        List<Object> listOfIntegers = (List<Object>) document.get("listOfIntegers");
        assertEquals(3,listOfIntegers.size());
        assertEquals(1, listOfIntegers.get(0));
        assertEquals(2, listOfIntegers.get(1));
        assertEquals(3, listOfIntegers.get(2));

        List<Object> listOfStrings = (List<Object>) document.get("listOfStrings");
        assertEquals(3, listOfStrings.size());
        assertEquals("a", listOfStrings.get(0));
        assertEquals("b", listOfStrings.get(1));
        assertEquals("c", listOfStrings.get(2));
    }
}
