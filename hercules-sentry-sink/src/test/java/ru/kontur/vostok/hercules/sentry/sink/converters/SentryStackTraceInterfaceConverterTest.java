package ru.kontur.vostok.hercules.sentry.sink.converters;

import io.sentry.event.interfaces.StackTraceInterface;
import org.junit.Assert;
import org.junit.Test;
import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.protocol.util.ContainerBuilder;
import ru.kontur.vostok.hercules.tags.StackFrameTags;

public class SentryStackTraceInterfaceConverterTest {

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
        StackTraceInterface stackTraceInterface = SentryStackTraceInterfaceConverter.convert(new Container[]{
                createFrame("a"),
                createFrame("b"),
                createFrame("c")
        });

        Assert.assertEquals(3, stackTraceInterface.getStackTrace().length);
        Assert.assertEquals("a", stackTraceInterface.getStackTrace()[0].getModule());
        Assert.assertEquals("b", stackTraceInterface.getStackTrace()[1].getModule());
        Assert.assertEquals("c", stackTraceInterface.getStackTrace()[2].getModule());
    }
}
