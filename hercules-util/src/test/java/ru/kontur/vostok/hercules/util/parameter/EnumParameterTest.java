package ru.kontur.vostok.hercules.util.parameter;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Gregory Koshelev
 */
public class EnumParameterTest {
    @Test
    public void optionalEnumParameterTest() {
        Parameter<ParameterType> enumParameter = Parameter.enumParameter("optional", ParameterType.class).build();

        ParameterValue<ParameterType> validValue = enumParameter.from("OPTIONAL");
        assertTrue(validValue.isOk());
        assertEquals(ParameterType.OPTIONAL, validValue.get());

        ParameterValue<ParameterType> invalidValue = enumParameter.from("optional");
        assertTrue(invalidValue.isError());

        ParameterValue<ParameterType> emptyValueFromEmptyString = enumParameter.from("");
        assertTrue(emptyValueFromEmptyString.isEmpty());

        ParameterValue<ParameterType> emptyValueFromNullString = enumParameter.from(null);
        assertTrue(emptyValueFromNullString.isEmpty());
    }

    @Test
    public void requiredEnumParameterTest() {
        Parameter<ParameterType> enumParameter = Parameter.enumParameter("required", ParameterType.class).required().build();

        ParameterValue<ParameterType> validValue = enumParameter.from("REQUIRED");
        assertTrue(validValue.isOk());
        assertEquals(ParameterType.REQUIRED, validValue.get());

        ParameterValue<ParameterType> invalidValue = enumParameter.from("required");
        assertTrue(invalidValue.isError());

        ParameterValue<ParameterType> invalidValueFromNullString = enumParameter.from(null);
        assertTrue(invalidValueFromNullString.isError());

        ParameterValue<ParameterType> invalidValueFromEmptyString = enumParameter.from("");
        assertTrue(invalidValueFromEmptyString.isError());
    }

    @Test
    public void defaultEnumParameterTest() {
        Parameter<ParameterType> enumParameter =
                Parameter.enumParameter("default", ParameterType.class).
                        withDefault(ParameterType.DEFAULT).
                        build();

        ParameterValue<ParameterType> validValue = enumParameter.from("REQUIRED");
        assertTrue(validValue.isOk());
        assertEquals(ParameterType.REQUIRED, validValue.get());

        ParameterValue<ParameterType> invalidValue = enumParameter.from("default");
        assertTrue(invalidValue.isError());

        ParameterValue<ParameterType> defaultValueFromNullString = enumParameter.from(null);
        assertTrue(defaultValueFromNullString.isOk());
        assertEquals(ParameterType.DEFAULT, defaultValueFromNullString.get());

        ParameterValue<ParameterType> defaultValueFromEmptyString = enumParameter.from("");
        assertTrue(defaultValueFromEmptyString.isOk());
        assertEquals(ParameterType.DEFAULT, defaultValueFromEmptyString.get());
    }
}
