package ru.kontur.vostok.hercules.util.uuid;

import org.junit.Assert;
import org.junit.Test;

import java.util.UUID;

/**
 * @author Petr Demenev
 */
public class UuidUtilTest {

    @Test
    public void isUuidTest() {
        Assert.assertTrue(UuidUtil.isUuid("01234567-89ab-cdef-0123-0123456789ab"));
        Assert.assertFalse(UuidUtil.isUuid("0123456789abcdef01230123456789ab"));
        Assert.assertFalse(UuidUtil.isUuid("0123456789abcdef"));
    }

    @Test
    public void isUuidWithoutHyphensTest() {
        Assert.assertFalse(UuidUtil.isUuidWithoutHyphens("01234567-89ab-cdef-0123-0123456789ab"));
        Assert.assertTrue(UuidUtil.isUuidWithoutHyphens("0123456789abcdef01230123456789ab"));
        Assert.assertFalse(UuidUtil.isUuidWithoutHyphens("0123456789abcdef"));
    }

    @Test
    public void shouldConvertStringToUuid() {
        Assert.assertEquals(
                UUID.fromString("01234567-89ab-cdef-0123-0123456789ab"),
                UuidUtil.fromString("0123456789abcdef01230123456789ab")
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionForInvalidString() {
        UuidUtil.fromString("0123456789abcdef");
    }

    @Test
    public void shouldConvertUuidToStringWithoutHyphens() {
        Assert.assertEquals(
                "0123456789abcdef01230123456789ab",
                UuidUtil.getUuidWithoutHyphens(UUID.fromString("01234567-89ab-cdef-0123-0123456789ab"))
        );
    }
}
