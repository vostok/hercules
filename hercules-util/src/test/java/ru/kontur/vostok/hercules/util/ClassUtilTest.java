package ru.kontur.vostok.hercules.util;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * @author Gregory Koshelev
 */
public class ClassUtilTest {
    @Test
    public void shouldCreateInstanceUsingDefaultConstructor() {
        I a = ClassUtil.fromClass(A.class.getName(), I.class);
        assertTrue(a instanceof A);
    }

    @Test
    public void shouldCreateInstanceUsingConstructorWithParameters() {
        I b = ClassUtil.fromClass(B.class.getName(), I.class, new Class<?>[]{String.class}, new String[]{"stub"});
        assertTrue(b instanceof B);
    }

    private interface I {
    }

    private static class A implements I {
        public A() {
        }
    }

    private static class B implements I {
        public B(String s) {
        }
    }
}
