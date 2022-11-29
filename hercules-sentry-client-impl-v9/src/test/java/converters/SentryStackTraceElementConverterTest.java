package converters;

import io.sentry.event.interfaces.SentryStackTraceElement;
import org.junit.Assert;
import org.junit.Test;
import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.sentry.client.impl.v9.SentryStackTraceElementConverter;
import ru.kontur.vostok.hercules.tags.StackFrameTags;

public class SentryStackTraceElementConverterTest {

    @Test
    public void shouldConvert() {

        final Container container = Container.builder()
                .tag(StackFrameTags.TYPE_TAG.getName(), Variant.ofString("test.module"))
                .tag(StackFrameTags.FUNCTION_TAG.getName(), Variant.ofString("testFunction"))
                .tag(StackFrameTags.FILE_TAG.getName(), Variant.ofString("SomeFile.java"))
                .tag(StackFrameTags.LINE_NUMBER_TAG.getName(), Variant.ofInteger(123))
                .tag(StackFrameTags.COLUMN_NUMBER_TAG.getName(), Variant.ofShort((short) 456))
                .build();

        final SentryStackTraceElement result = SentryStackTraceElementConverter.convert(container);

        Assert.assertEquals("test.module", result.getModule());
        Assert.assertEquals("testFunction", result.getFunction());
        Assert.assertEquals("SomeFile.java", result.getFileName());
        Assert.assertEquals(123, result.getLineno());
        Assert.assertEquals(456, (int) result.getColno());
    }
}
