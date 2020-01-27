package ru.kontur.vostok.hercules.elastic.sink.index;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Vladimir Tsypaev
 */
public class IndexValidatorTest {

    private static final int MAX_INDEX_SIZE_BYTES = 120;

    @Test
    public void shouldBeFalseIfIndexStartsWithForbiddenCharacters() {
        Assert.assertFalse(IndexValidator.isValidIndexName("_abc"));
        Assert.assertFalse(IndexValidator.isValidIndexName("-acb"));
        Assert.assertFalse(IndexValidator.isValidIndexName("+abc"));
        Assert.assertFalse(IndexValidator.isValidIndexName(".abc"));
        Assert.assertFalse(IndexValidator.isValidIndexName("..abc"));
    }

    @Test
    public void shouldBeFalseIfSizeOfIndexMoreThenMax() {
        Assert.assertFalse(IndexValidator.isValidLength(getString(MAX_INDEX_SIZE_BYTES + 1)));
    }

    @Test
    public void shouldBeTrueIfSizeOfIndexEqualsOrLessMax() {
        Assert.assertTrue(IndexValidator.isValidLength(getString(MAX_INDEX_SIZE_BYTES)));
        Assert.assertTrue(IndexValidator.isValidLength(getString(MAX_INDEX_SIZE_BYTES - 1)));
    }

    private String getString(int length) {
        return new String(new char[length]).replace('\0', 'a');
    }
}
