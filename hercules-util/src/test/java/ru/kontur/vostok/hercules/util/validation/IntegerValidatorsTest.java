package ru.kontur.vostok.hercules.util.validation;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Gregory Koshelev
 */
public class IntegerValidatorsTest {
    @Test
    public void shouldValidatePositive() {
        Validator<Integer> validator = IntegerValidators.positive();

        assertFalse(validator.validate(1).isError());
        assertTrue(validator.validate(0).isError());
        assertTrue(validator.validate(-1).isError());
    }

    @Test
    public void shouldValidateNonNegative() {
        Validator<Integer> validator = IntegerValidators.nonNegative();

        assertFalse(validator.validate(1).isError());
        assertFalse(validator.validate(0).isError());
        assertTrue(validator.validate(-1).isError());
    }
}
