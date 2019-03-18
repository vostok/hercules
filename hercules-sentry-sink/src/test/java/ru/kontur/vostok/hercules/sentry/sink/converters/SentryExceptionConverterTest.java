package ru.kontur.vostok.hercules.sentry.sink.converters;

import io.sentry.event.interfaces.SentryException;
import org.junit.Assert;
import org.junit.Test;
import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.protocol.Vector;
import ru.kontur.vostok.hercules.protocol.util.ContainerBuilder;
import ru.kontur.vostok.hercules.tags.ExceptionTags;
import ru.kontur.vostok.hercules.tags.StackFrameTags;

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
                .tag(StackFrameTags.TYPE_TAG, Variant.ofString(moduleName))
                .tag(StackFrameTags.FUNCTION_TAG, Variant.ofString("testFunction"))
                .tag(StackFrameTags.FILE_TAG, Variant.ofString("SomeFile.java"))
                .tag(StackFrameTags.LINE_NUMBER_TAG, Variant.ofInteger(123))
                .tag(StackFrameTags.COLUMN_NUMBER_TAG, Variant.ofShort((short) 456))
                .build();
    }

    @Test
    public void shouldConvert() throws Exception {
        Container container = ContainerBuilder.create()
                .tag(ExceptionTags.TYPE_TAG, Variant.ofString("test.module.SomeExceptionClass"))
                .tag(ExceptionTags.MESSAGE_TAG, Variant.ofString("Exception message"))
                .tag(ExceptionTags.STACK_FRAMES, Variant.ofVector(Vector.ofContainers(createStacktrace(2))))
                .build();

        SentryException exception = SentryExceptionConverter.convert(container);

        Assert.assertEquals("Exception message", exception.getExceptionMessage());
        Assert.assertEquals("SomeExceptionClass", exception.getExceptionClassName());
        Assert.assertEquals("test.module", exception.getExceptionPackageName());
        Assert.assertEquals(2, exception.getStackTraceInterface().getStackTrace().length);
    }
}
