package ru.kontur.vostok.hercules.protocol.util;

import org.junit.Assert;
import org.junit.Test;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.protocol.Vector;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @author Petr Demenev
 */
public class VariantUtilTest {

    @Test
    public void shouldExtractByte() {
        byte value = (byte) 5;
        Variant variant = Variant.ofByte(value);

        byte result = (byte) VariantUtil.extract(variant).get();

        Assert.assertEquals(value, result);
    }

    @Test
    public void shouldExtractShort() {
        short value = (short) 5;
        Variant variant = Variant.ofShort(value);

        short result = (short) VariantUtil.extract(variant).get();

        Assert.assertEquals(value, result);
    }

    @Test
    public void shouldExtractInt() {
        int value = 5;
        Variant variant = Variant.ofInteger(value);

        int result = (int) VariantUtil.extract(variant).get();

        Assert.assertEquals(value, result);
    }

    @Test
    public void shouldExtractLong() {
        long value = 5;
        Variant variant = Variant.ofLong(value);

        long result = (long) VariantUtil.extract(variant).get();

        Assert.assertEquals(value, result);
    }

    @Test
    public void shouldExtractFlag() {
        boolean value = true;
        Variant variant = Variant.ofFlag(value);

        boolean result = (boolean) VariantUtil.extract(variant).get();

        Assert.assertEquals(value, result);
    }

    @Test
    public void shouldExtractFloat() {
        float value = 5;
        Variant variant = Variant.ofFloat(value);

        float result = (float) VariantUtil.extract(variant).get();

        Assert.assertEquals(value, result, 0.01);
    }

    @Test
    public void shouldExtractUuid() {
        UUID value = UUID.randomUUID();
        Variant variant = Variant.ofUuid(value);

        UUID result = (UUID) VariantUtil.extract(variant).get();

        Assert.assertEquals(value, result);
    }

    @Test
    public void shouldExtractString() {
        String value = "String";
        Variant variant = Variant.ofString(value);

        String result = (String) VariantUtil.extract(variant).get();

        Assert.assertEquals(value, result);
    }

    @Test
    public void shouldExtractMap() {
        String stringKey = "String key";
        String stringValue = "String value";
        String intKey = "int key";
        int intValue = 5;
        Map<String, Object> map = new HashMap<>();
        map.put(stringKey, stringValue);
        map.put(intKey, intValue);
        Variant variant = Variant.ofContainer(ContainerBuilder.create()
                .tag(stringKey, Variant.ofString(stringValue))
                .tag(intKey, Variant.ofInteger(intValue))
                .build());

        Map<String, Object> result = (Map<String, Object>) VariantUtil.extract(variant).get();

        Assert.assertEquals(stringValue, result.get(stringKey));
        Assert.assertEquals(intValue, result.get(intKey));
    }

    @Test
    public void shouldExtractList() {
        String[] array = {"word1", "word2"};
        Variant variant = Variant.ofVector(Vector.ofStrings(array));

        List<String> result = (List<String>) VariantUtil.extract(variant).get();

        Assert.assertArrayEquals(array, result.toArray());
    }
}
