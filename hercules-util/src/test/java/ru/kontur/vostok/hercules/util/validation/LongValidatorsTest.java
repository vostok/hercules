package ru.kontur.vostok.hercules.util.validation;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Gregory Koshelev
 */
public class LongValidatorsTest {
    @Test
    public void shouldValidatePositive() {
        Validator<Long> validator = LongValidators.positive();

        assertFalse(validator.validate(1L).isError());
        assertTrue(validator.validate(0L).isError());
        assertTrue(validator.validate(-1L).isError());
    }

    @Test
    public void shouldValidateNonNegative() {
        Validator<Long> validator = LongValidators.nonNegative();

        assertFalse(validator.validate(1L).isError());
        assertFalse(validator.validate(0L).isError());
        assertTrue(validator.validate(-1L).isError());
    }
}
