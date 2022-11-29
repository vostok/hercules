package converters;

import io.sentry.event.interfaces.SentryException;
import io.sentry.event.interfaces.SentryStackTraceElement;
import org.junit.Assert;
import org.junit.Test;
import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.protocol.Vector;
import ru.kontur.vostok.hercules.sentry.client.impl.v9.SentryExceptionConverter;
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
        return Container.builder()
                .tag(StackFrameTags.TYPE_TAG.getName(), Variant.ofString(moduleName))
                .tag(StackFrameTags.FUNCTION_TAG.getName(), Variant.ofString("testFunction"))
                .tag(StackFrameTags.FILE_TAG.getName(), Variant.ofString("SomeFile.java"))
                .tag(StackFrameTags.LINE_NUMBER_TAG.getName(), Variant.ofInteger(123))
                .tag(StackFrameTags.COLUMN_NUMBER_TAG.getName(), Variant.ofShort((short) 456))
                .build();
    }

    @Test
    public void shouldConvert() {
        Container container = Container.builder()
                .tag(ExceptionTags.TYPE_TAG.getName(), Variant.ofString("test.module.SomeExceptionClass"))
                .tag(ExceptionTags.MESSAGE_TAG.getName(), Variant.ofString("Exception message"))
                .tag(ExceptionTags.STACK_FRAMES.getName(), Variant.ofVector(Vector.ofContainers(createStacktrace(2))))
                .build();

        SentryException exception = SentryExceptionConverter.convert(container);

        Assert.assertEquals("Exception message", exception.getExceptionMessage());
        Assert.assertEquals("SomeExceptionClass", exception.getExceptionClassName());
        Assert.assertEquals("test.module", exception.getExceptionPackageName());
        Assert.assertEquals(2, exception.getStackTraceInterface().getStackTrace().length);
    }

    @Test
    public void shouldConvertIfStacktraceIsEmpty() {
        Container container = Container.builder()
                .tag(ExceptionTags.TYPE_TAG.getName(), Variant.ofString("test.module.SomeExceptionClass"))
                .tag(ExceptionTags.MESSAGE_TAG.getName(), Variant.ofString("Exception message"))
                .tag(ExceptionTags.STACK_FRAMES.getName(), Variant.ofVector(Vector.ofContainers(createStacktrace(0))))
                .build();

        SentryException exception = SentryExceptionConverter.convert(container);

        SentryStackTraceElement[] elements = exception.getStackTraceInterface().getStackTrace();
        Assert.assertEquals(1, elements.length);
        SentryStackTraceElement emptyElement =
                new SentryStackTraceElement("", "", "", 0, null, "", null);
        Assert.assertEquals(emptyElement, elements[0]);
    }
}
