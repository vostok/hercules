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
import ru.kontur.vostok.hercules.protocol.util.EventBuilder;
import ru.kontur.vostok.hercules.util.application.ApplicationContextHolder;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class SentryEventConverterTest {

    private static Container createException() {
        Map<String, Variant> stackFrame0Map = new HashMap<>();
        stackFrame0Map.put("mod", Variant.ofString("com.example.test.SomeModule"));
        stackFrame0Map.put("fun", Variant.ofString("function"));
        stackFrame0Map.put("fnm", Variant.ofString("SomeModule.java"));
        stackFrame0Map.put("ln", Variant.ofInteger(100));
        stackFrame0Map.put("cn", Variant.ofInteger(12));
        stackFrame0Map.put("abs", Variant.ofString("/home/usr/git/project/src/java/com/example/test/SomeModule.java"));

        Map<String, Variant> stackFrame1Map = new HashMap<>();
        stackFrame1Map.put("mod", Variant.ofString("com.example.test.AnotherModule"));
        stackFrame1Map.put("fun", Variant.ofString("function"));
        stackFrame1Map.put("fnm", Variant.ofString("AnotherModule.java"));
        stackFrame1Map.put("ln", Variant.ofInteger(200));
        stackFrame1Map.put("cn", Variant.ofInteger(13));
        stackFrame1Map.put("abs", Variant.ofString("/home/usr/git/project/src/java/com/example/test/AnotherModule.java"));

        Map<String, Variant> exceptionMap = new HashMap<>();
        exceptionMap.put("str", Variant.ofVector(Vector.ofContainers(new Container[]{
                new Container(stackFrame0Map),
                new Container(stackFrame1Map)
        })));
        exceptionMap.put("tp", Variant.ofString("ExceptionClass"));
        exceptionMap.put("msg", Variant.ofString("Some error of ExceptionClass happened"));
        exceptionMap.put("mod", Variant.ofString("com.example.test.exceptions"));

        return new Container(exceptionMap);
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
        EventBuilder eventBuilder = EventBuilder.create(0, "00000000-0000-1000-994f-8fcf383f0000")//TODO: fix me
            .tag("msg", Variant.ofString("This is message sample"));

        Event sentryEvent = SentryEventConverter.convert(eventBuilder.build());

        Assert.assertEquals("This is message sample", sentryEvent.getMessage());
    }

    @Test
    public void shouldConvertEventWithExceptions() throws Exception {
        EventBuilder eventBuilder = EventBuilder.create(0, "00000000-0000-1000-994f-8fcf383f0000")//TODO: fix me
            .tag("exc", Variant.ofVector(Vector.ofContainers(createException())));

        Event sentryEvent = SentryEventConverter.convert(eventBuilder.build());

        ExceptionInterface exceptionInteface = (ExceptionInterface) sentryEvent.getSentryInterfaces().get("sentry.interfaces.Exception");
        SentryException exception = exceptionInteface.getExceptions().getFirst();

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
        Assert.assertEquals("/home/usr/git/project/src/java/com/example/test/SomeModule.java", stackTrace[0].getAbsPath());

        Assert.assertEquals("com.example.test.AnotherModule", stackTrace[1].getModule());
        Assert.assertEquals("function", stackTrace[1].getFunction());
        Assert.assertEquals("AnotherModule.java", stackTrace[1].getFileName());
        Assert.assertEquals(200, stackTrace[1].getLineno());
        Assert.assertEquals(13, (int) stackTrace[1].getColno());
        Assert.assertEquals("/home/usr/git/project/src/java/com/example/test/AnotherModule.java", stackTrace[1].getAbsPath());
    }

    @Test
    public void shouldExtractPlatformValue() throws Exception {
        EventBuilder eventBuilder = EventBuilder.create(0, "00000000-0000-1000-994f-8fcf383f0000") //TODO Fix me
            .tag("exc", Variant.ofVector(Vector.ofContainers(createException())));

        Event sentryEvent = SentryEventConverter.convert(eventBuilder.build());

        Assert.assertEquals("java", sentryEvent.getPlatform());
    }
}