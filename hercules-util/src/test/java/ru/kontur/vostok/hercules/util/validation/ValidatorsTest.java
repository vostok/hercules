package ru.kontur.vostok.hercules.util.validation;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * @author Gregory Koshelev
 */
public class ValidatorsTest {
    @Test
    public void shouldAcceptAnyValues() {
        Validator<Integer> validator = Validators.any();

        ValidationResult nullIsValid = validator.validate(null);
        assertTrue(nullIsValid.isOk());

        ValidationResult zeroIsValid = validator.validate(0);
        assertTrue(zeroIsValid.isOk());

        ValidationResult positiveIdValid = validator.validate(42);
        assertTrue(positiveIdValid.isOk());

        ValidationResult negativeIsValid = validator.validate(-42);
        assertTrue(negativeIsValid.isOk());
    }
}
