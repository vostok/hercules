package ru.kontur.vostok.hercules.sentry.sink.converters;

import io.sentry.event.interfaces.SentryException;
import org.junit.Assert;
import org.junit.Test;
import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.protocol.Vector;
import ru.kontur.vostok.hercules.protocol.util.ContainerBuilder;

import java.util.LinkedList;
import java.util.List;

public class SentryExceptionConverterTest {

    private static Container[] createStacktrace(int size) {
        List<Container> result = new LinkedList<>();
        for (int i = 0; i < size; i++) {
            result.add(createFrame(String.valueOf(i)));
        }
        return result.toArray(new Container[0]);
    }

    private static Container createFrame(String moduleName) {
        return ContainerBuilder.create()
                .tag("mod", Variant.ofString(moduleName))
                .tag("fun", Variant.ofString("testFunction"))
                .tag("fnm", Variant.ofString("SomeFile.java"))
                .tag("ln", Variant.ofInteger(123))
                .tag("cn", Variant.ofShort((short) 456))
                .tag("abs", Variant.ofString("/just/some/path/to/SomeFile.java"))
                .build();
    }

    @Test
    public void shouldConvert() throws Exception {
        Container container = ContainerBuilder.create()
                .tag("tp", Variant.ofString("SomeExceptionClass"))
                .tag("msg", Variant.ofString("Exception message"))
                .tag("mod", Variant.ofString("test.module"))
                .tag("str", Variant.ofVector(Vector.ofContainers(createStacktrace(2))))
                .build();

        SentryException exception = SentryExceptionConverter.convert(container);

        Assert.assertEquals("Exception message", exception.getExceptionMessage());
        Assert.assertEquals("SomeExceptionClass", exception.getExceptionClassName());
        Assert.assertEquals("test.module", exception.getExceptionPackageName());
        Assert.assertEquals(2, exception.getStackTraceInterface().getStackTrace().length);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowOnMissingType() throws Exception {
        SentryExceptionConverter.convert(
                ContainerBuilder.create()
                        .build()
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowOnMissingValue() throws Exception {
        SentryExceptionConverter.convert(
                ContainerBuilder.create()
                        .tag("tp", Variant.ofString("test"))
                        .build()
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowOnMissingModule() throws Exception {
        SentryExceptionConverter.convert(
                ContainerBuilder.create()
                        .tag("t", Variant.ofString("test"))
                        .tag("val", Variant.ofString("test"))
                        .build()
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowOnMissingStacktrace() throws Exception {
        SentryExceptionConverter.convert(
                ContainerBuilder.create()
                        .tag("tp", Variant.ofString("test"))
                        .tag("val", Variant.ofString("test"))
                        .tag("mod", Variant.ofString("test"))
                        .build()
        );
    }
}
