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

        Parameter<ParameterType>.ParameterValue validValue = enumParameter.from("OPTIONAL");
        assertTrue(validValue.isOk());
        assertEquals(ParameterType.OPTIONAL, validValue.get());

        Parameter<ParameterType>.ParameterValue invalidValue = enumParameter.from("optional");
        assertTrue(invalidValue.isError());

        Parameter<ParameterType>.ParameterValue emptyValueFromEmptyString = enumParameter.from("");
        assertTrue(emptyValueFromEmptyString.isEmpty());

        Parameter<ParameterType>.ParameterValue emptyValueFromNullString = enumParameter.from(null);
        assertTrue(emptyValueFromNullString.isEmpty());
    }

    @Test
    public void requiredEnumParameterTest() {
        Parameter<ParameterType> enumParameter = Parameter.enumParameter("required", ParameterType.class).required().build();

        Parameter<ParameterType>.ParameterValue validValue = enumParameter.from("REQUIRED");
        assertTrue(validValue.isOk());
        assertEquals(ParameterType.REQUIRED, validValue.get());

        Parameter<ParameterType>.ParameterValue invalidValue = enumParameter.from("required");
        assertTrue(invalidValue.isError());

        Parameter<ParameterType>.ParameterValue invalidValueFromNullString = enumParameter.from(null);
        assertTrue(invalidValueFromNullString.isError());

        Parameter<ParameterType>.ParameterValue invalidValueFromEmptyString = enumParameter.from("");
        assertTrue(invalidValueFromEmptyString.isError());
    }

    @Test
    public void defaultEnumParameterTest() {
        Parameter<ParameterType> enumParameter =
                Parameter.enumParameter("default", ParameterType.class).
                        withDefault(ParameterType.DEFAULT).
                        build();

        Parameter<ParameterType>.ParameterValue validValue = enumParameter.from("REQUIRED");
        assertTrue(validValue.isOk());
        assertEquals(ParameterType.REQUIRED, validValue.get());

        Parameter<ParameterType>.ParameterValue invalidValue = enumParameter.from("default");
        assertTrue(invalidValue.isError());

        Parameter<ParameterType>.ParameterValue defaultValueFromNullString = enumParameter.from(null);
        assertTrue(defaultValueFromNullString.isOk());
        assertEquals(ParameterType.DEFAULT, defaultValueFromNullString.get());

        Parameter<ParameterType>.ParameterValue defaultValueFromEmptyString = enumParameter.from("");
        assertTrue(defaultValueFromEmptyString.isOk());
        assertEquals(ParameterType.DEFAULT, defaultValueFromEmptyString.get());
    }
}
