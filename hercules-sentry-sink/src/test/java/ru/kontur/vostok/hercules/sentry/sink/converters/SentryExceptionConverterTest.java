package ru.kontur.vostok.hercules.sentry.sink.converters;

import io.sentry.event.interfaces.SentryException;
import org.junit.Assert;
import org.junit.Test;
import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Variant;
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
                .field("module", Variant.ofText(moduleName))
                .field("function", Variant.ofString("testFunction"))
                .field("filename", Variant.ofString("SomeFile.java"))
                .field("lineno", Variant.ofInteger(123))
                .field("colno", Variant.ofShort((short) 456))
                .field("abs_path", Variant.ofText("/just/some/path/to/SomeFile.java"))
                .build();
    }

    @Test
    public void shouldConvert() throws Exception {
        Container container = ContainerBuilder.create()
                .field("type", Variant.ofString("SomeExceptionClass"))
                .field("value", Variant.ofText("Exception message"))
                .field("module", Variant.ofText("test.module"))
                .field("stacktrace", Variant.ofContainerArray(createStacktrace(2)))
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
                        .field("type", Variant.ofText("test"))
                        .build()
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowOnMissingModule() throws Exception {
        SentryExceptionConverter.convert(
                ContainerBuilder.create()
                        .field("type", Variant.ofText("test"))
                        .field("value", Variant.ofText("test"))
                        .build()
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowOnMissingStacktrace() throws Exception {
        SentryExceptionConverter.convert(
                ContainerBuilder.create()
                        .field("type", Variant.ofText("test"))
                        .field("value", Variant.ofText("test"))
                        .field("module", Variant.ofText("test"))
                        .build()
        );
    }
}
