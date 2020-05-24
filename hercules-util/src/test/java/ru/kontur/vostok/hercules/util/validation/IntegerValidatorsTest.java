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

        assertTrue(validator.validate(null).isError());
        assertFalse(validator.validate(1).isError());
        assertTrue(validator.validate(0).isError());
        assertTrue(validator.validate(-1).isError());
    }

    @Test
    public void shouldValidateNonNegative() {
        Validator<Integer> validator = IntegerValidators.nonNegative();

        assertTrue(validator.validate(null).isError());
        assertFalse(validator.validate(1).isError());
        assertFalse(validator.validate(0).isError());
        assertTrue(validator.validate(-1).isError());
    }

    @Test
    public void shouldAcceptNonNullValuesOnly() {
        Validator<Integer> validator = Validators.notNull();

        assertTrue(validator.validate(null).isError());
        assertTrue(validator.validate(0).isOk());
        assertTrue(validator.validate(42).isOk());
        assertTrue(validator.validate(-42).isOk());
    }

    @Test
    public void shouldAcceptInRangeValues() {
        int left = -128;
        int right = 128;

        Validator<Integer> validator = IntegerValidators.range(left, right);

        assertTrue(validator.validate(null).isError());
        assertTrue(validator.validate(left - 1).isError());
        assertTrue(validator.validate(left).isOk());
        assertTrue(validator.validate(right - 1).isOk());
        assertTrue(validator.validate(right).isError());
    }

    @Test
    public void shouldAcceptInRangeValuesInclusively() {
        int left = -128;
        int right = 127;

        Validator<Integer> validator = IntegerValidators.rangeInclusive(left, right);

        assertTrue(validator.validate(null).isError());
        assertTrue(validator.validate(left - 1).isError());
        assertTrue(validator.validate(left).isOk());
        assertTrue(validator.validate(right).isOk());
        assertTrue(validator.validate(right + 1).isError());
    }
}
