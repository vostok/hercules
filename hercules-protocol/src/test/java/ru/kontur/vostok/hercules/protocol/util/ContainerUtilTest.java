package ru.kontur.vostok.hercules.protocol.util;

import org.junit.Assert;
import org.junit.Test;
import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.protocol.Vector;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Petr Demenev
 */
public class ContainerUtilTest {

    private final String stringTag = "string_tag";
    private final String stringValue = "string_value";
    private final String nullTag = "null_tag";
    private final String vectorTag = "vector_tag";
    private final String[] vectorValue = {"word1", "word2"};
    private final String innerConainerTag = "container_tag";
    private final Map<String, Object> innerConainerMapValue = new HashMap<>();
    private final String intTag = "int_tag";
    private final int intValue = 123;
    private final String booleanTag = "boolean_tag";
    private final boolean booleanValue = true;

    private final Container container = (Container) Variant.ofContainer(ContainerBuilder.create()
            .tag(stringTag, Variant.ofString(stringValue))
            .tag(nullTag, Variant.ofNull())
            .tag(vectorTag, Variant.ofVector(Vector.ofStrings(vectorValue)))
            .tag(innerConainerTag, Variant.ofContainer(ContainerBuilder.create()
                    .tag(intTag, Variant.ofInteger(intValue))
                    .tag(booleanTag, Variant.ofFlag(booleanValue))
                    .build()
            ))
            .build())
            .getValue();

    @Test
    public void shouldConvertContainerToMap() {
        Map<String, Object> resultMap = ContainerUtil.toObjectMap(container);

        Assert.assertEquals(stringValue, resultMap.get(stringTag));
        Assert.assertNull(resultMap.get(nullTag));
        List<String> strings = (List<String>) resultMap.get(vectorTag);
        Assert.assertArrayEquals(vectorValue, strings.toArray());
        Map<String, Object> innerConainerMapValue = (Map<String, Object>) resultMap.get(innerConainerTag);
        Assert.assertEquals(intValue, innerConainerMapValue.get(intTag));
        Assert.assertEquals(booleanValue, innerConainerMapValue.get(booleanTag));
    }

    @Test
    public void shouldConvertContainerToMapWithExclusions() {
        Set<String> exclusions = new HashSet<>();
        exclusions.add(stringTag);
        exclusions.add(innerConainerTag);

        Map<String, Object> resultMap = ContainerUtil.toObjectMap(container, exclusions);

        Assert.assertEquals(2, resultMap.size());
        Assert.assertTrue(resultMap.containsKey(nullTag));
        Assert.assertTrue(resultMap.containsKey(vectorTag));
        Assert.assertFalse(resultMap.containsKey(stringTag));
        Assert.assertFalse(resultMap.containsKey(innerConainerTag));
    }
}
