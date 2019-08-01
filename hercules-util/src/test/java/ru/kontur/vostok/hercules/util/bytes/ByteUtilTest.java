package ru.kontur.vostok.hercules.util.bytes;

import org.junit.Assert;
import org.junit.Test;

import java.nio.ByteBuffer;

/**
 * @author Petr Demenev
 */
public class ByteUtilTest {
    private static final String RAW_HEX_STRING = "0123456789ABCDEFabcdef";
    private static final String PREFIX = "0x";
    private static final String HEX_STRING = PREFIX + RAW_HEX_STRING;
    private static final String EXPECTED_HEX_STRING = PREFIX + RAW_HEX_STRING.toUpperCase();
    private static final byte[] BYTE_ARRAY = new byte[]{
            (byte) 0x01,
            (byte) 0x23,
            (byte) 0x45,
            (byte) 0x67,
            (byte) 0x89,
            (byte) 0xAB,
            (byte) 0xCD,
            (byte) 0xEF,
            (byte) 0xAB,
            (byte) 0xCD,
            (byte) 0xEF};
    private static final byte[] BYTE_ARRAY_LENGTH_8 = new byte[]{
            (byte) 0x01,
            (byte) 0x23,
            (byte) 0x45,
            (byte) 0x67,
            (byte) 0x89,
            (byte) 0xAB,
            (byte) 0xCD,
            (byte) 0xEF};

    @Test
    public void shouldReturnTrueForSubarray() {
        boolean result = ByteUtil.isSubarray(BYTE_ARRAY, BYTE_ARRAY_LENGTH_8);

        Assert.assertTrue(result);
    }

    @Test
    public void shouldReturnFalseForNonSubarray() {
        boolean result = ByteUtil.isSubarray(BYTE_ARRAY, new byte[] {0x23, 0x01});

        Assert.assertFalse(result);
    }

    @Test
    public void shouldReturnFalseIfArrayDoesNotStartWithSubarray() {
        boolean result = ByteUtil.isSubarray(BYTE_ARRAY, new byte[] {0x23, 0x45});

        Assert.assertFalse(result);
    }

    @Test
    public void shouldConvertBytesToLong() {
        long result = ByteUtil.toLong(BYTE_ARRAY_LENGTH_8);

        Assert.assertEquals(81985529216486895L, result);
    }

    @Test (expected = IllegalArgumentException.class)
    public void shouldThrowIllegalArgumentExceptionIfNumberOfBytesNotEqualsTo8() {
        ByteUtil.toLong(BYTE_ARRAY);
    }

    @Test
    public void shouldConvertBytesToHexString() {
        String result = ByteUtil.toHexString(BYTE_ARRAY);

        Assert.assertEquals(EXPECTED_HEX_STRING, result);
    }

    @Test
    public void shouldConvertByteBufferToHexString() {
        String result = ByteUtil.toHexString(ByteBuffer.wrap(BYTE_ARRAY));

        Assert.assertEquals(EXPECTED_HEX_STRING, result);

    }

    @Test
    public void shouldConvertHexStringToBytes() {
        byte[] result = ByteUtil.fromHexString(HEX_STRING);

        Assert.assertArrayEquals(BYTE_ARRAY, result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowIllegalArgumentExceptionIfStringDoesNotContainHexPrefix() {
        String sourceString = "1234";

        ByteUtil.fromHexString(sourceString);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowIllegalArgumentExceptionIfStringContainsOddNumberOfCharacters() {
        String sourceString = "0x12345";

        ByteUtil.fromHexString(sourceString);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowIllegalArgumentExceptionIfStringContainsNonHexCharacters() {
        String sourceString = "0xABCDEFG";

        ByteUtil.fromHexString(sourceString);
    }
}