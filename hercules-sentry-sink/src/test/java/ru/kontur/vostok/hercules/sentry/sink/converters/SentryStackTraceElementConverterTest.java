package ru.kontur.vostok.hercules.sentry.sink.converters;

import io.sentry.event.interfaces.SentryStackTraceElement;
import org.junit.Assert;
import org.junit.Test;
import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.protocol.util.ContainerBuilder;
import ru.kontur.vostok.hercules.tags.StackFrameTags;

public class SentryStackTraceElementConverterTest {

    @Test
    public void shouldConvert() throws Exception {

        final Container container = ContainerBuilder.create()
                .tag(StackFrameTags.TYPE_TAG, Variant.ofString("test.module"))
                .tag(StackFrameTags.FUNCTION_TAG, Variant.ofString("testFunction"))
                .tag(StackFrameTags.FILE_TAG, Variant.ofString("SomeFile.java"))
                .tag(StackFrameTags.LINE_NUMBER_TAG, Variant.ofInteger(123))
                .tag(StackFrameTags.COLUMN_NUMBER_TAG, Variant.ofShort((short) 456))
                .build();

        final SentryStackTraceElement result = SentryStackTraceElementConverter.convert(container);

        Assert.assertEquals("test.module", result.getModule());
        Assert.assertEquals("testFunction", result.getFunction());
        Assert.assertEquals("SomeFile.java", result.getFileName());
        Assert.assertEquals(123, result.getLineno());
        Assert.assertEquals(456, (int) result.getColno());
    }
}
