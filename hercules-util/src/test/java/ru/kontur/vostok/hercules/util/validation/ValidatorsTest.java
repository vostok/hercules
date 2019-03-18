package ru.kontur.vostok.hercules.util.validation;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class ValidatorsTest {

    @Test
    public void shouldValidateGreaterThanRestriction() throws Exception {
        Validator<Integer> validator = Validators.greaterThan(0);

        assertFalse(validator.validate(1).isPresent());
        assertEquals("Value must be greater than '0' but was '0'", validator.validate(0).get());
        assertEquals("Value must be greater than '0' but was '-1'", validator.validate(-1).get());
    }

    @Test
    public void shouldValidateGreaterOrEqualsRestriction() throws Exception {
        Validator<Integer> validator = Validators.greaterOrEquals(0);

        assertFalse(validator.validate(1).isPresent());
        assertFalse(validator.validate(0).isPresent());
        assertEquals("Value must be greater or equals '0' but was '-1'", validator.validate(-1).get());
    }
    @Test
    public void shouldValidateLesserThanRestriction() throws Exception {
        Validator<Integer> validator = Validators.lesserThan(0);

        assertEquals("Value must be lesser than '0' but was '1'", validator.validate(1).get());
        assertEquals("Value must be lesser than '0' but was '0'", validator.validate(0).get());
        assertFalse(validator.validate(-1).isPresent());
    }

    @Test
    public void shouldValidateLesserOrEqualsRestriction() throws Exception {
        Validator<Integer> validator = Validators.lesserOrEquals(0);

        assertEquals("Value must be lesser or equals '0' but was '1'", validator.validate(1).get());
        assertFalse(validator.validate(0).isPresent());
        assertFalse(validator.validate(-1).isPresent());
    }
}
