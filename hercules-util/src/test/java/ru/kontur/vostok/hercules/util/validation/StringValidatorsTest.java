package ru.kontur.vostok.hercules.util.validation;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * @author Petr Demenev
 */
public class StringValidatorsTest {
    @Test
    public void shouldValidateNotEmptyString() {
        Validator<String> validator = StringValidators.notEmpty();

        assertFalse(validator.validate("string").isError());
        assertEquals("String is empty", validator.validate("").error());
        assertEquals("String is empty", validator.validate(null).error());
    }

    @Test
    public void shouldValidateMatchesWithPattern() {
        Validator<String> validator = StringValidators.matchesWith("[a-z0-9_]{1,48}");

        assertFalse(validator.validate("string_123").isError());
        assertFalse(validator.validate("this_testing_string_contains_forty_eight_symbols").isError());
        assertEquals("String should match the pattern '[a-z0-9_]{1,48}' but was 'a b'",
                validator.validate("a b").error());
        assertEquals("String should match the pattern '[a-z0-9_]{1,48}' but was ''",
                validator.validate("").error());
        assertEquals("String should match the pattern '[a-z0-9_]{1,48}' "
                        + "but was 'this_testing_string_contains_forty_nine_symbols__'",
                validator.validate("this_testing_string_contains_forty_nine_symbols__").error());

    }
}
