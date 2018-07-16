package ru.kontur.vostok.hercules.elastic.adapter;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.kontur.vostok.hercules.elastic.adapter.util.ElasticAdapterUtil;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.HerculesProtocolAssert;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.protocol.encoder.EventBuilder;
import ru.kontur.vostok.hercules.uuid.UuidGenerator;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class ElasticAdapterUtilTest {
    private final UuidGenerator generator = UuidGenerator.getClientInstance();

    private String jsonStringVariant = "{ \"A\": \"A\"}";
    private String jsonIntVariant = "{ \"B\": 1}";
    private String jsonIntArrayVariant = "{ \"C\": [1, 2, 3, 4]}";
    private String jsonStringArrayVariant = "{ \"D\": [\"11\", \"22\", \"33\", \"44\"]}";
    private String jsonEmptyArrayVariant = "{ \"E\": []}";
    private String jsonDoubleVariant = "{ \"F\": 1.25}";
    private String jsonDoubleArrayVariant = "{ \"G\": [1.25, 2.34]}";
    private String jsonEmptyVariant = "{}";
    private String jsonComplexVariant = "{" +
            "\"HA\": 1," +
            "\"HB\": 1.25," +
            "\"HC\": \"str\"," +
            "\"HD\": [1, 2, 3, 4]" +
            "}";
    private String multiJson = jsonStringVariant +
            jsonIntVariant +
            jsonIntArrayVariant +
            jsonStringArrayVariant +
            jsonEmptyArrayVariant +
            jsonDoubleVariant +
            jsonDoubleArrayVariant +
            jsonEmptyVariant +
            jsonComplexVariant;

    private Event[] events;
    private int count;

    @Before
    public void setUp() {
        count = 9;
        events = new Event[count];

        events[0] = buildEvent(Collections.singletonMap("A", Variant.ofString("A")));
        events[1] = buildEvent(Collections.singletonMap("B", Variant.ofInteger(1)));
        events[2] = buildEvent(Collections.singletonMap("C", Variant.ofIntegerArray(new int[]{1, 2, 3, 4})));
        events[3] = buildEvent(Collections.singletonMap("D", Variant.ofStringArray(new String[]{"11", "22", "33", "44"})));
        events[4] = buildEvent(Collections.singletonMap("E", Variant.ofStringArray(new String[0])));
        events[5] = buildEvent(Collections.singletonMap("F", Variant.ofDouble(1.25)));
        events[6] = buildEvent(Collections.singletonMap("G", Variant.ofDoubleArray(new double[]{1.25, 2.34})));
        events[7] = buildEvent(new HashMap<>());

        Map<String, Variant> map = new HashMap<>();
        map.put("HA", Variant.ofInteger(1));
        map.put("HB", Variant.ofDouble(1.25));
        map.put("HC", Variant.ofString("str"));
        map.put("HD", Variant.ofIntegerArray(new int[] {1, 2, 3, 4}));
        events[8] = buildEvent(map);
    }


    @Test
    public void ParseSimpleEvent_StringVariant() throws IOException {
        shouldCreateCorrectEvents(jsonStringVariant, 0, 1);
    }

    @Test
    public void ParseSimpleEvent_IntVariant() throws IOException {
        shouldCreateCorrectEvents(jsonIntVariant, 1, 1);
    }

    @Test
    public void ParseSimpleEvent_IntArrayVariant() throws IOException {
        shouldCreateCorrectEvents(jsonIntArrayVariant, 2, 1);
    }

    @Test
    public void ParseSimpleEvent_StringArrayVariant() throws IOException {
        shouldCreateCorrectEvents(jsonStringArrayVariant, 3, 1);
    }

    @Test
    public void ParseSimpleEvent_EmptyArrayVariant() throws IOException {
        shouldCreateCorrectEvents(jsonEmptyArrayVariant, 4, 1);
    }

    @Test
    public void ParseSimpleEvent_DoubleVariant() throws IOException {
        shouldCreateCorrectEvents(jsonDoubleVariant, 5, 1);
    }

    @Test
    public void ParseSimpleEvent_DoubleArrayVariant() throws IOException {
        shouldCreateCorrectEvents(jsonDoubleArrayVariant, 6, 1);
    }

    @Test
    public void ParseSimpleEvent_Empty() throws IOException {
        shouldCreateCorrectEvents(jsonEmptyVariant, 7, 1);
    }

    @Test
    public void ParseComplexEvent_ComplexVariant() throws IOException {
        shouldCreateCorrectEvents(jsonComplexVariant, 8, 1);
    }

    @Test
    public void ParseEvents_MultiJson() throws IOException {
        shouldCreateCorrectEvents(multiJson, 0, count);
    }

    private void shouldCreateCorrectEvents(String json, int offset, int count) throws IOException {
        Event[] events = ElasticAdapterUtil
                .createEventStream(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)))
                .toArray(Event[]::new);

        Assert.assertEquals(count, events.length);

        for (int index = 0; index < count; index++ ) {
            eventsSame(this.events[index + offset], events[index]);
        }

    }

    private void eventsSame(Event event1, Event event2) {
        Set<String> keys1 = new HashSet<>();

        event1.forEach(entry -> {
            HerculesProtocolAssert.assertEquals(entry.getValue(), event2.getTag(entry.getKey()));
            keys1.add(entry.getKey());
        });

        event2.forEach(entry -> Assert.assertTrue(keys1.contains(entry.getKey())));

        Assert.assertEquals(event1.getVersion(), event2.getVersion());
    }

    private Event buildEvent(Map<String, Variant> map) {
        EventBuilder eventBuilder = new EventBuilder();
        eventBuilder.setVersion(1);
        eventBuilder.setEventId(generator.next());
        map.forEach(eventBuilder::setTag);

        return eventBuilder.build();
    }
}
