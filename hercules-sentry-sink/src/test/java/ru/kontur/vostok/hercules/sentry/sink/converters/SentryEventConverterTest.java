package ru.kontur.vostok.hercules.sentry.sink.converters;

import io.sentry.event.Event;
import io.sentry.event.interfaces.ExceptionInterface;
import io.sentry.event.interfaces.SentryException;
import io.sentry.event.interfaces.SentryStackTraceElement;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.protocol.Vector;
import ru.kontur.vostok.hercules.protocol.util.ContainerBuilder;
import ru.kontur.vostok.hercules.protocol.util.EventBuilder;
import ru.kontur.vostok.hercules.tags.ExceptionTags;
import ru.kontur.vostok.hercules.tags.LogEventTags;
import ru.kontur.vostok.hercules.tags.StackFrameTags;
import ru.kontur.vostok.hercules.util.application.ApplicationContextHolder;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class SentryEventConverterTest {

    private static Container createException() {
        return ContainerBuilder.create()
            .tag(ExceptionTags.TYPE_TAG, Variant.ofString("com.example.test.exceptions.ExceptionClass"))
            .tag(ExceptionTags.MESSAGE_TAG, Variant.ofString("Some error of ExceptionClass happened"))
            .tag(ExceptionTags.STACK_FRAMES, Variant.ofVector(Vector.ofContainers(
                ContainerBuilder.create()
                    .tag(StackFrameTags.TYPE_TAG, Variant.ofString("com.example.test.SomeModule"))
                    .tag(StackFrameTags.FUNCTION_TAG, Variant.ofString("function"))
                    .tag(StackFrameTags.FILE_TAG, Variant.ofString("SomeModule.java"))
                    .tag(StackFrameTags.LINE_NUMBER_TAG, Variant.ofInteger(100))
                    .tag(StackFrameTags.COLUMN_NUMBER_TAG, Variant.ofShort((short) 12))
                    .build(),
                ContainerBuilder.create()
                    .tag(StackFrameTags.TYPE_TAG, Variant.ofString("com.example.test.AnotherModule"))
                    .tag(StackFrameTags.FUNCTION_TAG, Variant.ofString("function"))
                    .tag(StackFrameTags.FILE_TAG, Variant.ofString("AnotherModule.java"))
                    .tag(StackFrameTags.LINE_NUMBER_TAG, Variant.ofInteger(200))
                    .tag(StackFrameTags.COLUMN_NUMBER_TAG, Variant.ofShort((short) 13))
                    .build()
            )))
            .build();
    }

    @Before
    public void setUp() throws Exception {
        Properties testProperties = new Properties();
        testProperties.setProperty("environment", "test");
        testProperties.setProperty("instance.id", "test");
        testProperties.setProperty("zone", "test");
        ApplicationContextHolder.init("test","test", testProperties);
    }

    @Test
    public void shouldConvertEventWithMessage() throws Exception {
        final ru.kontur.vostok.hercules.protocol.Event event = EventBuilder
            .create(0, "00000000-0000-1000-994f-8fcf383f0000")
            .tag(LogEventTags.MESSAGE_TAG, Variant.ofString("This is message sample"))
            .build();

        final Event sentryEvent = SentryEventConverter.convert(event);

        Assert.assertEquals("This is message sample", sentryEvent.getMessage());
    }

    @Test
    public void shouldConvertEventWithExceptions() throws Exception {
        final ru.kontur.vostok.hercules.protocol.Event event = EventBuilder
            .create(0, "00000000-0000-1000-994f-8fcf383f0000")
            .tag(LogEventTags.EXCEPTION_TAG, Variant.ofContainer(createException()))
            .build();

        Event sentryEvent = SentryEventConverter.convert(event);

        ExceptionInterface exceptionInterface = (ExceptionInterface) sentryEvent.getSentryInterfaces().get("sentry.interfaces.Exception");
        SentryException exception = exceptionInterface.getExceptions().getFirst();

        Assert.assertEquals("Some error of ExceptionClass happened", exception.getExceptionMessage());
        Assert.assertEquals("ExceptionClass", exception.getExceptionClassName());
        Assert.assertEquals("com.example.test.exceptions", exception.getExceptionPackageName());

        SentryStackTraceElement[] stackTrace = exception.getStackTraceInterface().getStackTrace();
        Assert.assertEquals(2, stackTrace.length);

        Assert.assertEquals("com.example.test.SomeModule", stackTrace[0].getModule());
        Assert.assertEquals("function", stackTrace[0].getFunction());
        Assert.assertEquals("SomeModule.java", stackTrace[0].getFileName());
        Assert.assertEquals(100, stackTrace[0].getLineno());
        Assert.assertEquals(12, (int) stackTrace[0].getColno());
        Assert.assertEquals("SomeModule.java", stackTrace[0].getAbsPath());

        Assert.assertEquals("com.example.test.AnotherModule", stackTrace[1].getModule());
        Assert.assertEquals("function", stackTrace[1].getFunction());
        Assert.assertEquals("AnotherModule.java", stackTrace[1].getFileName());
        Assert.assertEquals(200, stackTrace[1].getLineno());
        Assert.assertEquals(13, (int) stackTrace[1].getColno());
        Assert.assertEquals("AnotherModule.java", stackTrace[1].getAbsPath());
    }

    @Test
    public void shouldExtractPlatformValue() throws Exception {
        final ru.kontur.vostok.hercules.protocol.Event event = EventBuilder
            .create(0, "00000000-0000-1000-994f-8fcf383f0000") //TODO Fix me
            .tag("exc", Variant.ofVector(Vector.ofContainers(createException())))
            .build();

        final Event sentryEvent = SentryEventConverter.convert(event);

        Assert.assertEquals("java", sentryEvent.getPlatform());
    }
}
