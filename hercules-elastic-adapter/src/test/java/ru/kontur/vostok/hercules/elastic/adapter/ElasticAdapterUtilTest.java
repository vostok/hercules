package ru.kontur.vostok.hercules.elastic.adapter;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.kontur.vostok.hercules.elastic.adapter.util.ElasticAdapterUtil;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.protocol.encoder.EventBuilder;
import ru.kontur.vostok.hercules.uuid.UuidGenerator;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ElasticAdapterUtilTest {
    private final UuidGenerator generator = UuidGenerator.getClientInstance();

    private String json =
            "{ \"A\": \"A\"}" +
                    "{ \"B\": 1}" +
                    "{ \"C\": [1, 2, 3, 4]}" +
                    "{ \"D\": [\"11\", \"22\", \"33\", \"44\"]}" +
                    "{ \"E\": []}" +
                    "{ \"F\": 1.25}" +
                    "{ \"G\": [1.25, 2.34]}";

    private Event[] events;
    private Map<String, Variant>[] maps;
    private int count;

    @Before
    public void setUp() {
        count = 7;
        maps = new HashMap[count];
        events = new Event[count];

        maps[0] = new HashMap<>();
        maps[0].put("A", Variant.ofString("A"));

        maps[1] = new HashMap<>();
        maps[1].put("B", Variant.ofInteger(1));

        maps[2] = new HashMap<>();
        maps[2].put("C", Variant.ofIntegerArray(new int[]{1, 2, 3, 4}));

        maps[3] = new HashMap<>();
        maps[3].put("D", Variant.ofStringArray(new String[]{"11", "22", "33", "44"}));

        maps[4] = new HashMap<>();
        maps[4].put("E", Variant.ofStringArray(new String[0]));

        maps[5] = new HashMap<>();
        maps[5].put("F", Variant.ofDouble(1.25));

        maps[6] = new HashMap<>();
        maps[6].put("G", Variant.ofDoubleArray(new double[]{1.25, 2.34}));

        for (int i = 0; i < count; i++) {
            events[i] = buildEvent(maps[i]);
        }

    }


    @Test
    public void testLogic() throws IOException {
        Event[] events = ElasticAdapterUtil.createEvents(json);
        for (int index = 0; index < count; index++) {
            Assert.assertTrue(eventsSame(this.events[index], events[index]));
        }
    }

    private boolean eventsSame(Event event_1, Event event_2) {
        return event_1.getVersion() == event_1.getVersion() &&
                mapEqual(event_1.getTags(), event_2.getTags());
    }

    private boolean mapEqual(Map<String, Variant> map_1, Map<String, Variant> map_2) {
        for (Map.Entry<String, Variant> entry : map_1.entrySet()) {
            String key = entry.getKey();
            Variant variant = entry.getValue();

            if (variant == null) {
                if (!(map_2.get(key) == null && map_2.containsKey(key))) {
                    return false;
                }
            } else {
                if (!variantEqual(variant, map_2.get(key))) {
                    return false;
                }
            }
        }

        return true;
    }

    private boolean variantEqual(Variant variant_1, Variant variant_2) {
        return variant_1.getType().value == variant_2.getType().value
                && Objects.deepEquals(variant_1.getValue(), variant_2.getValue());
    }

    private Event buildEvent(Map<String, Variant> map) {
        EventBuilder eventBuilder = new EventBuilder();
        eventBuilder.setVersion(1);
        eventBuilder.setEventId(generator.next());
        map.forEach(eventBuilder::setTag);

        return eventBuilder.build();
    }
}
