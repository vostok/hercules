package ru.kontur.vostok.hercules.util.parameter;

import org.junit.Test;
import ru.kontur.vostok.hercules.util.parameter.parsing.Parsers;
import ru.kontur.vostok.hercules.util.validation.IntegerValidators;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Gregory Koshelev
 */
public class ParameterTest {

    @Test
    public void optionalBooleanParameterTest() {
        Parameter<Boolean> booleanParameter = Parameter.booleanParameter("optional").build();

        ParameterValue<Boolean> trueValue = booleanParameter.from("true");
        assertTrue(trueValue.isOk());
        assertTrue(trueValue.get());

        ParameterValue<Boolean> falseValue = booleanParameter.from("false");
        assertTrue(falseValue.isOk());
        assertFalse(falseValue.get());

        ParameterValue<Boolean> invalidValue = booleanParameter.from("qwerty");
        assertTrue(invalidValue.isError());

        ParameterValue<Boolean> emptyValueFromNullString = booleanParameter.from(null);
        assertTrue(emptyValueFromNullString.isEmpty());

        ParameterValue<Boolean> emptyValueFromEmptyString = booleanParameter.from("");
        assertTrue(emptyValueFromEmptyString.isEmpty());
    }


    @Test
    public void optionalShortParameterTest() {
        Parameter<Short> shortParameter = Parameter.shortParameter("optional").build();

        ParameterValue<Short> validValue = shortParameter.from("42");
        assertTrue(validValue.isOk());
        assertEquals(42, (short) validValue.get());

        ParameterValue<Short> invalidValue = shortParameter.from("1000000");
        assertTrue(invalidValue.isError());

        ParameterValue<Short> invalidValueFromNonDigitString = shortParameter.from("qwerty");
        assertTrue(invalidValueFromNonDigitString.isError());

        ParameterValue<Short> emptyValueFromNullString = shortParameter.from(null);
        assertTrue(emptyValueFromNullString.isEmpty());

        ParameterValue<Short> emptyValueFromEmptyString = shortParameter.from("");
        assertTrue(emptyValueFromEmptyString.isEmpty());
    }


    @Test
    public void optionalIntegerParameterTest() {
        Parameter<Integer> integerParameter = Parameter.integerParameter("optional").build();

        ParameterValue<Integer> validValue = integerParameter.from("42");
        assertTrue(validValue.isOk());
        assertEquals(42, (int) validValue.get());

        ParameterValue<Integer> invalidValueFromNonDigitString = integerParameter.from("qwerty");
        assertTrue(invalidValueFromNonDigitString.isError());

        ParameterValue<Integer> emptyValueFromNullString = integerParameter.from(null);
        assertTrue(emptyValueFromNullString.isEmpty());

        ParameterValue<Integer> emptyValueFromEmptyString = integerParameter.from("");
        assertTrue(emptyValueFromEmptyString.isEmpty());
    }

    @Test
    public void optionalLongParameterTest() {
        Parameter<Long> longParameter = Parameter.longParameter("optional").build();

        ParameterValue<Long> validValue = longParameter.from("42");
        assertTrue(validValue.isOk());
        assertEquals(42L, (long) validValue.get());

        ParameterValue<Long> invalidValueFromNonDigitString = longParameter.from("qwerty");
        assertTrue(invalidValueFromNonDigitString.isError());

        ParameterValue<Long> emptyValueFromNullString = longParameter.from(null);
        assertTrue(emptyValueFromNullString.isEmpty());

        ParameterValue<Long> emptyValueFromEmptyString = longParameter.from("");
        assertTrue(emptyValueFromEmptyString.isEmpty());
    }

    @Test
    public void optionalStringParameterTest() {
        Parameter<String> stringParameter = Parameter.stringParameter("optional").build();

        ParameterValue<String> validValue = stringParameter.from("qwerty");
        assertTrue(validValue.isOk());
        assertEquals("qwerty", validValue.get());

        ParameterValue<String> emptyStringIsValidValue = stringParameter.from("");
        assertTrue(emptyStringIsValidValue.isOk());
        assertEquals("", emptyStringIsValidValue.get());

        ParameterValue<String> emptyValue = stringParameter.from(null);
        assertTrue(emptyValue.isEmpty());
    }

    @Test
    public void requiredBooleanParameterTest() {
        Parameter<Boolean> booleanParameter = Parameter.booleanParameter("required").required().build();

        ParameterValue<Boolean> validValue = booleanParameter.from("true");
        assertTrue(validValue.isOk());
        assertTrue(validValue.get());

        ParameterValue<Boolean> invalidValueFromNullString = booleanParameter.from(null);
        assertTrue(invalidValueFromNullString.isError());

        ParameterValue<Boolean> invalidValueFromEmptyString = booleanParameter.from("");
        assertTrue(invalidValueFromEmptyString.isError());
    }

    @Test
    public void requiredShortParameterTest() {
        Parameter<Short> shortParameter = Parameter.shortParameter("required").required().build();

        ParameterValue<Short> validValue = shortParameter.from("42");
        assertTrue(validValue.isOk());
        assertEquals(42, (int) validValue.get());

        ParameterValue<Short> invalidValueFromNullString = shortParameter.from(null);
        assertTrue(invalidValueFromNullString.isError());

        ParameterValue<Short> invalidValueFromEmptyString = shortParameter.from("");
        assertTrue(invalidValueFromEmptyString.isError());
    }

    @Test
    public void requiredIntegerParameterTest() {
        Parameter<Integer> integerParameter = Parameter.integerParameter("required").required().build();

        ParameterValue<Integer> validValue = integerParameter.from("42");
        assertTrue(validValue.isOk());
        assertEquals(42, (int) validValue.get());

        ParameterValue<Integer> invalidValueFromNullString = integerParameter.from(null);
        assertTrue(invalidValueFromNullString.isError());

        ParameterValue<Integer> invalidValueFromEmptyString = integerParameter.from("");
        assertTrue(invalidValueFromEmptyString.isError());
    }

    @Test
    public void requiredLongParameterTest() {
        Parameter<Long> longParameter = Parameter.longParameter("required").required().build();

        ParameterValue<Long> validValue = longParameter.from("42");
        assertTrue(validValue.isOk());
        assertEquals(42L, (long) validValue.get());

        ParameterValue<Long> invalidValueFromNullString = longParameter.from(null);
        assertTrue(invalidValueFromNullString.isError());

        ParameterValue<Long> invalidValueFromEmptyString = longParameter.from("");
        assertTrue(invalidValueFromEmptyString.isError());
    }

    @Test
    public void requiredStringParameterTest() {
        Parameter<String> stringParameter = Parameter.stringParameter("required").required().build();

        ParameterValue<String> validValue = stringParameter.from("string");
        assertTrue(validValue.isOk());
        assertEquals("string", validValue.get());

        ParameterValue<String> invalidValueFromNullString = stringParameter.from(null);
        assertTrue(invalidValueFromNullString.isError());

        ParameterValue<String> validValueFromEmptyString = stringParameter.from("");
        assertTrue(validValueFromEmptyString.isOk());
        assertEquals("", validValueFromEmptyString.get());
    }

    @Test
    public void defaultBooleanParameterTest() {
        Parameter<Boolean> booleanParameter = Parameter.booleanParameter("default").withDefault(false).build();

        ParameterValue<Boolean> validValue = booleanParameter.from("true");
        assertTrue(validValue.isOk());
        assertTrue(validValue.get());

        ParameterValue<Boolean> defaultValueFromNullString = booleanParameter.from(null);
        assertTrue(defaultValueFromNullString.isOk());
        assertFalse(defaultValueFromNullString.get());

        ParameterValue<Boolean> defaultValueFromEmptyString = booleanParameter.from("");
        assertTrue(defaultValueFromEmptyString.isOk());
        assertFalse(defaultValueFromEmptyString.get());
    }

    @Test
    public void defaultShortParameterTest() {
        Parameter<Short> shortParameter = Parameter.shortParameter("default").withDefault((short) 1).build();

        ParameterValue<Short> validValue = shortParameter.from("42");
        assertTrue(validValue.isOk());
        assertEquals(42, (short) validValue.get());

        ParameterValue<Short> defaultValueFromNullString = shortParameter.from(null);
        assertTrue(defaultValueFromNullString.isOk());
        assertEquals(1, (short) defaultValueFromNullString.get());

        ParameterValue<Short> defaultValueFromEmptyString = shortParameter.from("");
        assertTrue(defaultValueFromEmptyString.isOk());
        assertEquals(1, (short) defaultValueFromEmptyString.get());
    }

    @Test
    public void defaultIntegerParameterTest() {
        Parameter<Integer> integerParameter = Parameter.integerParameter("default").withDefault(1).build();

        ParameterValue<Integer> validValue = integerParameter.from("42");
        assertTrue(validValue.isOk());
        assertEquals(42, (int) validValue.get());

        ParameterValue<Integer> defaultValueFromNullString = integerParameter.from(null);
        assertTrue(defaultValueFromNullString.isOk());
        assertEquals(1, (int) defaultValueFromNullString.get());

        ParameterValue<Integer> defaultValueFromEmptyString = integerParameter.from("");
        assertTrue(defaultValueFromEmptyString.isOk());
        assertEquals(1, (int) defaultValueFromEmptyString.get());
    }

    @Test
    public void defaultLongParameterTest() {
        Parameter<Long> integerParameter = Parameter.longParameter("default").withDefault(1L).build();

        ParameterValue<Long> validValue = integerParameter.from("42");
        assertTrue(validValue.isOk());
        assertEquals(42L, (long) validValue.get());

        ParameterValue<Long> defaultValueFromNullString = integerParameter.from(null);
        assertTrue(defaultValueFromNullString.isOk());
        assertEquals(1L, (long) defaultValueFromNullString.get());

        ParameterValue<Long> defaultValueFromEmptyString = integerParameter.from("");
        assertTrue(defaultValueFromEmptyString.isOk());
        assertEquals(1L, (long) defaultValueFromEmptyString.get());
    }

    @Test
    public void defaultStringParameterTest() {
        Parameter<String> stringParameter = Parameter.stringParameter("default").withDefault("default").build();

        ParameterValue<String> validValue = stringParameter.from("string");
        assertTrue(validValue.isOk());
        assertEquals("string", validValue.get());

        ParameterValue<String> defaultValueFromNullString = stringParameter.from(null);
        assertTrue(defaultValueFromNullString.isOk());
        assertEquals("default", defaultValueFromNullString.get());

        ParameterValue<String> validValueFromEmptyString = stringParameter.from("");
        assertTrue(validValueFromEmptyString.isOk());
        assertEquals("", validValueFromEmptyString.get());
    }

    @Test
    public void parameterValidationTest() {
        Parameter<Integer> integerParameter =
                Parameter.integerParameter("positive").
                        required().
                        withValidator(IntegerValidators.positive()).
                        build();

        ParameterValue<Integer> validValue = integerParameter.from("42");
        assertTrue(validValue.isOk());

        ParameterValue<Integer> invalidValue = integerParameter.from("-5");
        assertTrue(invalidValue.isError());
    }

    @Test(expected = IllegalStateException.class)
    public void shouldPreventInvalidDefaultValueTest() {
        Parameter<Integer> invalidParameter =
                Parameter.integerParameter("invalid").
                        withDefault(-5).
                        withValidator(IntegerValidators.positive()).
                        build();
    }

    @Test
    public void optionalParameterValidationTest() {
        Parameter<Integer> integerParameter =
                Parameter.integerParameter("optional").
                        withValidator(IntegerValidators.positive()).
                        build();

        ParameterValue<Integer> emptyValueIsValid = integerParameter.from("");
        assertTrue(emptyValueIsValid.isOk());
        assertTrue(emptyValueIsValid.isEmpty());
    }

    @Test
    public void parameterWithFunctionParserTest() {
        Parameter<UUID> uuidParameter =
                Parameter.parameter("uuid", Parsers.fromFunction(UUID::fromString)).
                        build();

        assertTrue(uuidParameter.from("").isEmpty());

        ParameterValue<UUID> uuid = uuidParameter.from("11203800-63FD-11E8-83E2-3A587D902000");
        assertTrue(uuid.isOk());
        assertEquals(UUID.fromString("11203800-63FD-11E8-83E2-3A587D902000"), uuid.get());
    }
}
