package ru.kontur.vostok.hercules.util.parameter;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Gregory Koshelev
 */
public class ArrayParameterTest {
    @Test
    public void optionalStringArrayParameterTest() {
        Parameter<String[]> stringArrayParameter = Parameter.stringArrayParameter("optional").build();

        ParameterValue<String[]> singleValue = stringArrayParameter.from("qwerty");
        assertTrue(singleValue.isOk());
        assertEquals(1, singleValue.get().length);
        assertEquals("qwerty", singleValue.get()[0]);

        ParameterValue<String[]> multipleValue = stringArrayParameter.from("qwerty,123456");
        assertTrue(multipleValue.isOk());
        assertEquals(2, multipleValue.get().length);
        assertEquals("qwerty", multipleValue.get()[0]);
        assertEquals("123456", multipleValue.get()[1]);

        ParameterValue<String[]> emptyValueFromEmptyString = stringArrayParameter.from("");
        assertTrue(emptyValueFromEmptyString.isEmpty());

        ParameterValue<String[]> emptyValueFromNullString = stringArrayParameter.from(null);
        assertTrue(emptyValueFromNullString.isEmpty());
    }

    @Test
    public void requiredStringArrayParameterTest() {
        Parameter<String[]> stringArrayParameter = Parameter.stringArrayParameter("required").required().build();

        ParameterValue<String[]> singleValue = stringArrayParameter.from("qwerty");
        assertTrue(singleValue.isOk());
        assertEquals(1, singleValue.get().length);
        assertEquals("qwerty", singleValue.get()[0]);

        ParameterValue<String[]> multipleValue = stringArrayParameter.from("qwerty,123456");
        assertTrue(multipleValue.isOk());
        assertEquals(2, multipleValue.get().length);
        assertEquals("qwerty", multipleValue.get()[0]);
        assertEquals("123456", multipleValue.get()[1]);

        ParameterValue<String[]> invalidValueFromNullString = stringArrayParameter.from(null);
        assertTrue(invalidValueFromNullString.isError());

        ParameterValue<String[]> invalidValueFromEmptyString = stringArrayParameter.from("");
        assertTrue(invalidValueFromEmptyString.isError());
    }

    @Test
    public void defaultStringArrayParameterTest() {
        Parameter<String[]> stringArrayParameter =
                Parameter.stringArrayParameter("default").
                        withDefault(new String[]{"default"}).
                        build();

        ParameterValue<String[]> singleValue = stringArrayParameter.from("qwerty");
        assertTrue(singleValue.isOk());
        assertEquals(1, singleValue.get().length);
        assertEquals("qwerty", singleValue.get()[0]);

        ParameterValue<String[]> multipleValue = stringArrayParameter.from("qwerty,123456");
        assertTrue(multipleValue.isOk());
        assertEquals(2, multipleValue.get().length);
        assertEquals("qwerty", multipleValue.get()[0]);
        assertEquals("123456", multipleValue.get()[1]);

        ParameterValue<String[]> defaultValueFromNullString = stringArrayParameter.from(null);
        assertTrue(defaultValueFromNullString.isOk());
        assertEquals(1, defaultValueFromNullString.get().length);
        assertEquals("default", defaultValueFromNullString.get()[0]);

        ParameterValue<String[]> defaultValueFromEmptyString = stringArrayParameter.from("");
        assertTrue(defaultValueFromEmptyString.isOk());
        assertEquals(1, defaultValueFromEmptyString.get().length);
        assertEquals("default", defaultValueFromEmptyString.get()[0]);
    }
}
