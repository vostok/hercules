package ru.kontur.vostok.hercules.util.validation;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Gregory Koshelev
 */
public class ArrayValidatorsTest {
    @Test
    public void shouldValidateNonEmptyArray() {
        Validator<String[]> validator = ArrayValidators.notEmpty();

        assertTrue(validator.validate(new String[0]).isPresent());
        assertFalse(validator.validate(new String[]{"a"}).isPresent());
    }
}
