package ru.kontur.vostok.hercules.sentry.sink.converters;

import io.sentry.event.interfaces.SentryStackTraceElement;
import org.junit.Assert;
import org.junit.Test;
import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.protocol.util.ContainerBuilder;

public class SentryStackTraceElementConverterTest {

    @Test
    public void shouldConvert() throws Exception {

        Container container = ContainerBuilder.create()
                .field("mod", Variant.ofText("test.module"))
                .field("fun", Variant.ofString("testFunction"))
                .field("fnm", Variant.ofString("SomeFile.java"))
                .field("ln", Variant.ofInteger(123))
                .field("cn", Variant.ofShort((short) 456))
                .field("abs", Variant.ofText("/just/some/path/to/SomeFile.java"))
                .build();

        SentryStackTraceElement result = SentryStackTraceElementConverter.convert(container);

        Assert.assertEquals("test.module", result.getModule());
        Assert.assertEquals("testFunction", result.getFunction());
        Assert.assertEquals("SomeFile.java", result.getFileName());
        Assert.assertEquals(123, result.getLineno());
        Assert.assertEquals(456, result.getColno().intValue());
        Assert.assertEquals("/just/some/path/to/SomeFile.java", result.getAbsPath());
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowOnMissingModule() throws Exception {
        SentryStackTraceElementConverter.convert(
                ContainerBuilder.create()
                        .build()
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowOnMissingFunction() throws Exception {
        SentryStackTraceElementConverter.convert(
                ContainerBuilder.create()
                        .field("mod", Variant.ofText("test.module"))
                        .build()
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowOnMissingFilename() throws Exception {
        SentryStackTraceElementConverter.convert(
                ContainerBuilder.create()
                        .field("mod", Variant.ofText("test.module"))
                        .field("fun", Variant.ofString("testFunction"))
                        .build()
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowOnMissingLineNumber() throws Exception {
        SentryStackTraceElementConverter.convert(
                ContainerBuilder.create()
                        .field("mod", Variant.ofText("test.module"))
                        .field("fun", Variant.ofString("testFunction"))
                        .field("fnm", Variant.ofString("SomeFile.java"))
                        .build()
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowOnMissingAbsPath() throws Exception {
        SentryStackTraceElementConverter.convert(
                ContainerBuilder.create()
                        .field("mod", Variant.ofText("test.module"))
                        .field("fun", Variant.ofString("testFunction"))
                        .field("fnm", Variant.ofString("SomeFile.java"))
                        .field("ln", Variant.ofInteger(123))
                        .build()
        );
    }
}
