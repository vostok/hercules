package ru.kontur.vostok.hercules.util.properties;

import org.junit.Test;

import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PropertiesUtilTest {

    @Test
    public void prettyView() throws Exception {
        final Properties properties = new Properties();
        properties.put("b.d.e", "345");
        properties.put("a.b.d", "234");
        properties.put("a.b.c", "123");

        String prettyView = PropertiesUtil.prettyView(properties);

        assertEquals(
                "\n" +
                "\ta.b.c=123\n" +
                "\ta.b.d=234\n" +
                "\tb.d.e=345",
                prettyView
        );
    }

    @Test
    public void shouldCreateInstanceListFromProperties() {
        final Properties properties = new Properties();
        properties.put("0.class", TestClass.class.getName());
        properties.put("0.props.a", "1");
        properties.put("0.props.b", "true");
        properties.put("1.class", TestClass.class.getName());
        properties.put("1.props.a", "2");
        properties.put("1.props.b", "false");

        List<TestClass> list = PropertiesUtil.createClassInstanceList(properties, TestClass.class);

        assertEquals(1, list.get(0).a);
        assertTrue(list.get(0).b);
        assertEquals(2, list.get(1).a);
        assertFalse(list.get(1).b);
    }

    @Test
    public void shouldCreateSingleInstanceFromProperties() {
        final Properties properties = new Properties();
        properties.put("class", TestClass.class.getName());
        properties.put("props.a", "1");
        properties.put("props.b", "true");

        TestClass instance = PropertiesUtil.createClassInstance(properties, TestClass.class);

        assertEquals(1, instance.a);
        assertTrue(instance.b);
    }

    @Test
    public void shouldCreateSingleInstanceFromEmptyProperties() {
        final Properties properties = new Properties();
        properties.put("class", TestClass.class.getName());

        TestClass instance = PropertiesUtil.createClassInstance(properties, TestClass.class);

        assertEquals(3, instance.a);
        assertTrue(instance.b);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowExceptionWhenWrongProperties() {
        PropertiesUtil.createClassInstance(new Properties(), TestClass.class);
    }

    public static class TestClass {
        int a;
        boolean b;

        public TestClass(Properties properties) {
            a = Integer.parseInt(properties.getProperty("a"));
            b = Boolean.parseBoolean(properties.getProperty("b"));
        }

        public TestClass() {
            a = 3;
            b = true;
        }
    }
}
