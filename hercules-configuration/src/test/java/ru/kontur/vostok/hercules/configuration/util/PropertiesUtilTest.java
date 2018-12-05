package ru.kontur.vostok.hercules.configuration.util;

import java.util.Properties;

import static org.junit.Assert.assertEquals;

public class PropertiesUtilTest {

    @org.junit.Test
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
}
