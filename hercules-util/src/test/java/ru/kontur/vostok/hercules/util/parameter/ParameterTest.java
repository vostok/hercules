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

        Parameter<Boolean>.ParameterValue trueValue = booleanParameter.from("true");
        assertTrue(trueValue.isOk());
        assertTrue(trueValue.get());

        Parameter<Boolean>.ParameterValue falseValue = booleanParameter.from("false");
        assertTrue(falseValue.isOk());
        assertFalse(falseValue.get());

        Parameter<Boolean>.ParameterValue invalidValue = booleanParameter.from("qwerty");
        assertTrue(invalidValue.isError());

        Parameter<Boolean>.ParameterValue emptyValueFromNullString = booleanParameter.from(null);
        assertTrue(emptyValueFromNullString.isEmpty());

        Parameter<Boolean>.ParameterValue emptyValueFromEmptyString = booleanParameter.from("");
        assertTrue(emptyValueFromEmptyString.isEmpty());
    }


    @Test
    public void optionalShortParameterTest() {
        Parameter<Short> shortParameter = Parameter.shortParameter("optional").build();

        Parameter<Short>.ParameterValue validValue = shortParameter.from("42");
        assertTrue(validValue.isOk());
        assertEquals(42, (short) validValue.get());

        Parameter<Short>.ParameterValue invalidValue = shortParameter.from("1000000");
        assertTrue(invalidValue.isError());

        Parameter<Short>.ParameterValue invalidValueFromNonDigitString = shortParameter.from("qwerty");
        assertTrue(invalidValueFromNonDigitString.isError());

        Parameter<Short>.ParameterValue emptyValueFromNullString = shortParameter.from(null);
        assertTrue(emptyValueFromNullString.isEmpty());

        Parameter<Short>.ParameterValue emptyValueFromEmptyString = shortParameter.from("");
        assertTrue(emptyValueFromEmptyString.isEmpty());
    }


    @Test
    public void optionalIntegerParameterTest() {
        Parameter<Integer> integerParameter = Parameter.integerParameter("optional").build();

        Parameter<Integer>.ParameterValue validValue = integerParameter.from("42");
        assertTrue(validValue.isOk());
        assertEquals(42, (int) validValue.get());

        Parameter<Integer>.ParameterValue invalidValueFromNonDigitString = integerParameter.from("qwerty");
        assertTrue(invalidValueFromNonDigitString.isError());

        Parameter<Integer>.ParameterValue emptyValueFromNullString = integerParameter.from(null);
        assertTrue(emptyValueFromNullString.isEmpty());

        Parameter<Integer>.ParameterValue emptyValueFromEmptyString = integerParameter.from("");
        assertTrue(emptyValueFromEmptyString.isEmpty());
    }

    @Test
    public void optionalLongParameterTest() {
        Parameter<Long> longParameter = Parameter.longParameter("optional").build();

        Parameter<Long>.ParameterValue validValue = longParameter.from("42");
        assertTrue(validValue.isOk());
        assertEquals(42L, (long) validValue.get());

        Parameter<Long>.ParameterValue invalidValueFromNonDigitString = longParameter.from("qwerty");
        assertTrue(invalidValueFromNonDigitString.isError());

        Parameter<Long>.ParameterValue emptyValueFromNullString = longParameter.from(null);
        assertTrue(emptyValueFromNullString.isEmpty());

        Parameter<Long>.ParameterValue emptyValueFromEmptyString = longParameter.from("");
        assertTrue(emptyValueFromEmptyString.isEmpty());
    }

    @Test
    public void optionalStringParameterTest() {
        Parameter<String> stringParameter = Parameter.stringParameter("optional").build();

        Parameter<String>.ParameterValue validValue = stringParameter.from("qwerty");
        assertTrue(validValue.isOk());
        assertEquals("qwerty", validValue.get());

        Parameter<String>.ParameterValue emptyStringIsValidValue = stringParameter.from("");
        assertTrue(emptyStringIsValidValue.isOk());
        assertEquals("", emptyStringIsValidValue.get());

        Parameter<String>.ParameterValue emptyValue = stringParameter.from(null);
        assertTrue(emptyValue.isEmpty());
    }

    @Test
    public void requiredBooleanParameterTest() {
        Parameter<Boolean> booleanParameter = Parameter.booleanParameter("required").required().build();

        Parameter<Boolean>.ParameterValue validValue = booleanParameter.from("true");
        assertTrue(validValue.isOk());
        assertTrue(validValue.get());

        Parameter<Boolean>.ParameterValue invalidValueFromNullString = booleanParameter.from(null);
        assertTrue(invalidValueFromNullString.isError());

        Parameter<Boolean>.ParameterValue invalidValueFromEmptyString = booleanParameter.from("");
        assertTrue(invalidValueFromEmptyString.isError());
    }

    @Test
    public void requiredShortParameterTest() {
        Parameter<Short> shortParameter = Parameter.shortParameter("required").required().build();

        Parameter<Short>.ParameterValue validValue = shortParameter.from("42");
        assertTrue(validValue.isOk());
        assertEquals(42, (int) validValue.get());

        Parameter<Short>.ParameterValue invalidValueFromNullString = shortParameter.from(null);
        assertTrue(invalidValueFromNullString.isError());

        Parameter<Short>.ParameterValue invalidValueFromEmptyString = shortParameter.from("");
        assertTrue(invalidValueFromEmptyString.isError());
    }

    @Test
    public void requiredIntegerParameterTest() {
        Parameter<Integer> integerParameter = Parameter.integerParameter("required").required().build();

        Parameter<Integer>.ParameterValue validValue = integerParameter.from("42");
        assertTrue(validValue.isOk());
        assertEquals(42, (int) validValue.get());

        Parameter<Integer>.ParameterValue invalidValueFromNullString = integerParameter.from(null);
        assertTrue(invalidValueFromNullString.isError());

        Parameter<Integer>.ParameterValue invalidValueFromEmptyString = integerParameter.from("");
        assertTrue(invalidValueFromEmptyString.isError());
    }

    @Test
    public void requiredLongParameterTest() {
        Parameter<Long> longParameter = Parameter.longParameter("required").required().build();

        Parameter<Long>.ParameterValue validValue = longParameter.from("42");
        assertTrue(validValue.isOk());
        assertEquals(42L, (long) validValue.get());

        Parameter<Long>.ParameterValue invalidValueFromNullString = longParameter.from(null);
        assertTrue(invalidValueFromNullString.isError());

        Parameter<Long>.ParameterValue invalidValueFromEmptyString = longParameter.from("");
        assertTrue(invalidValueFromEmptyString.isError());
    }

    @Test
    public void requiredStringParameterTest() {
        Parameter<String> stringParameter = Parameter.stringParameter("required").required().build();

        Parameter<String>.ParameterValue validValue = stringParameter.from("string");
        assertTrue(validValue.isOk());
        assertEquals("string", validValue.get());

        Parameter<String>.ParameterValue invalidValueFromNullString = stringParameter.from(null);
        assertTrue(invalidValueFromNullString.isError());

        Parameter<String>.ParameterValue validValueFromEmptyString = stringParameter.from("");
        assertTrue(validValueFromEmptyString.isOk());
        assertEquals("", validValueFromEmptyString.get());
    }

    @Test
    public void defaultBooleanParameterTest() {
        Parameter<Boolean> booleanParameter = Parameter.booleanParameter("default").withDefault(false).build();

        Parameter<Boolean>.ParameterValue validValue = booleanParameter.from("true");
        assertTrue(validValue.isOk());
        assertTrue(validValue.get());

        Parameter<Boolean>.ParameterValue defaultValueFromNullString = booleanParameter.from(null);
        assertTrue(defaultValueFromNullString.isOk());
        assertFalse(defaultValueFromNullString.get());

        Parameter<Boolean>.ParameterValue defaultValueFromEmptyString = booleanParameter.from("");
        assertTrue(defaultValueFromEmptyString.isOk());
        assertFalse(defaultValueFromEmptyString.get());
    }

    @Test
    public void defaultShortParameterTest() {
        Parameter<Short> shortParameter = Parameter.shortParameter("default").withDefault((short) 1).build();

        Parameter<Short>.ParameterValue validValue = shortParameter.from("42");
        assertTrue(validValue.isOk());
        assertEquals(42, (short) validValue.get());

        Parameter<Short>.ParameterValue defaultValueFromNullString = shortParameter.from(null);
        assertTrue(defaultValueFromNullString.isOk());
        assertEquals(1, (short) defaultValueFromNullString.get());

        Parameter<Short>.ParameterValue defaultValueFromEmptyString = shortParameter.from("");
        assertTrue(defaultValueFromEmptyString.isOk());
        assertEquals(1, (short) defaultValueFromEmptyString.get());
    }

    @Test
    public void defaultIntegerParameterTest() {
        Parameter<Integer> integerParameter = Parameter.integerParameter("default").withDefault(1).build();

        Parameter<Integer>.ParameterValue validValue = integerParameter.from("42");
        assertTrue(validValue.isOk());
        assertEquals(42, (int) validValue.get());

        Parameter<Integer>.ParameterValue defaultValueFromNullString = integerParameter.from(null);
        assertTrue(defaultValueFromNullString.isOk());
        assertEquals(1, (int) defaultValueFromNullString.get());

        Parameter<Integer>.ParameterValue defaultValueFromEmptyString = integerParameter.from("");
        assertTrue(defaultValueFromEmptyString.isOk());
        assertEquals(1, (int) defaultValueFromEmptyString.get());
    }

    @Test
    public void defaultLongParameterTest() {
        Parameter<Long> integerParameter = Parameter.longParameter("default").withDefault(1L).build();

        Parameter<Long>.ParameterValue validValue = integerParameter.from("42");
        assertTrue(validValue.isOk());
        assertEquals(42L, (long) validValue.get());

        Parameter<Long>.ParameterValue defaultValueFromNullString = integerParameter.from(null);
        assertTrue(defaultValueFromNullString.isOk());
        assertEquals(1L, (long) defaultValueFromNullString.get());

        Parameter<Long>.ParameterValue defaultValueFromEmptyString = integerParameter.from("");
        assertTrue(defaultValueFromEmptyString.isOk());
        assertEquals(1L, (long) defaultValueFromEmptyString.get());
    }

    @Test
    public void defaultStringParameterTest() {
        Parameter<String> stringParameter = Parameter.stringParameter("default").withDefault("default").build();

        Parameter<String>.ParameterValue validValue = stringParameter.from("string");
        assertTrue(validValue.isOk());
        assertEquals("string", validValue.get());

        Parameter<String>.ParameterValue defaultValueFromNullString = stringParameter.from(null);
        assertTrue(defaultValueFromNullString.isOk());
        assertEquals("default", defaultValueFromNullString.get());

        Parameter<String>.ParameterValue validValueFromEmptyString = stringParameter.from("");
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

        Parameter<Integer>.ParameterValue validValue = integerParameter.from("42");
        assertTrue(validValue.isOk());

        Parameter<Integer>.ParameterValue invalidValue = integerParameter.from("-5");
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

        Parameter<Integer>.ParameterValue emptyValueIsValid = integerParameter.from("");
        assertTrue(emptyValueIsValid.isOk());
        assertTrue(emptyValueIsValid.isEmpty());
    }

    @Test
    public void parameterWithFunctionParserTest() {
        Parameter<UUID> uuidParameter =
                Parameter.parameter("uuid", Parsers.fromFunction(UUID::fromString)).
                        build();

        assertTrue(uuidParameter.from("").isEmpty());

        Parameter<UUID>.ParameterValue uuid = uuidParameter.from("11203800-63FD-11E8-83E2-3A587D902000");
        assertTrue(uuid.isOk());
        assertEquals(UUID.fromString("11203800-63FD-11E8-83E2-3A587D902000"), uuid.get());
    }
}
