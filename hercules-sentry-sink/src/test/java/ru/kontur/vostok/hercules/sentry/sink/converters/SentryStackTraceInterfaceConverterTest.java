package ru.kontur.vostok.hercules.sentry.sink.converters;

import io.sentry.event.interfaces.StackTraceInterface;
import org.junit.Assert;
import org.junit.Test;
import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.protocol.util.ContainerBuilder;

public class SentryStackTraceInterfaceConverterTest {

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
