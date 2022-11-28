package ru.kontur.vostok.hercules.util.text;

import org.junit.Assert;
import org.junit.Test;

/**
 * Unit test class for {@link IgnoreCaseWrapper}.
 *
 * @author Aleksandr Yuferov
 */
public class IgnoreCaseWrapperTest {
    @Test
    public void equalsTest() {
        Assert.assertEquals(new IgnoreCaseWrapper<>("VaLuE"), new IgnoreCaseWrapper<>("value"));
    }

    @Test
    public void hashTest() {
        Assert.assertEquals(new IgnoreCaseWrapper<>("VaLuE").hashCode(), new IgnoreCaseWrapper<>("value").hashCode());
    }
}
