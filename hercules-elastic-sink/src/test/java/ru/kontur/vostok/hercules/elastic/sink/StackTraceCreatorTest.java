package ru.kontur.vostok.hercules.elastic.sink;

import org.junit.Assert;
import org.junit.Test;
import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.protocol.Vector;
import ru.kontur.vostok.hercules.tags.ExceptionTags;
import ru.kontur.vostok.hercules.tags.StackFrameTags;

/**
 * @author Petr Demenev
 */
public class StackTraceCreatorTest {

    @Test
    public void createStackTraceTest() {
        String expected =
                "com.example.test.exceptions.ExceptionClass: Some error of ExceptionClass happened\n" +
                "    at com.example.test.SomeModule.function(SomeModule.java:100:12)\n" +
                "    at com.example.test.AnotherModule.function(AnotherModule.java:200:13)\n" +
                "  Caused by: com.example.test.exceptions.FirstInnerExceptionClass: Some error of FirstInnerExceptionClass happened\n" +
                "      at com.example.test.SomeModule.function(SomeModule.java:100:12)\n" +
                "      at com.example.test.AnotherModule.function(AnotherModule.java:200:13)\n" +
                "  Caused by: com.example.test.exceptions.SecondInnerExceptionClass: Some error of SecondInnerExceptionClass happened\n" +
                "      at com.example.test.SomeModule.function(SomeModule.java:100:12)\n" +
                "      at com.example.test.AnotherModule.function(AnotherModule.java:200:13)";
        Assert.assertEquals(expected, StackTraceCreator.createStackTrace(createException()));
    }

    private Container createException() {
        return Container.builder()
                .tag(ExceptionTags.TYPE_TAG.getName(), Variant.ofString("com.example.test.exceptions.ExceptionClass"))
                .tag(ExceptionTags.MESSAGE_TAG.getName(), Variant.ofString("Some error of ExceptionClass happened"))
                .tag(ExceptionTags.STACK_FRAMES.getName(), Variant.ofVector(createStackFrames()))
                .tag(ExceptionTags.INNER_EXCEPTIONS_TAG.getName(), Variant.ofVector(Vector.ofContainers(
                        createInnerException("FirstInnerExceptionClass"),
                        createInnerException("SecondInnerExceptionClass")
                )))
                .build();
    }

    private Container createInnerException(String type) {
        return Container.builder()
                .tag(ExceptionTags.TYPE_TAG.getName(), Variant.ofString("com.example.test.exceptions." + type))
                .tag(ExceptionTags.MESSAGE_TAG.getName(), Variant.ofString("Some error of " + type + " happened"))
                .tag(ExceptionTags.STACK_FRAMES.getName(), Variant.ofVector(createStackFrames()))
                .build();
    }

    private Vector createStackFrames() {
        return Vector.ofContainers(
                Container.builder()
                        .tag(StackFrameTags.TYPE_TAG.getName(), Variant.ofString("com.example.test.SomeModule"))
                        .tag(StackFrameTags.FUNCTION_TAG.getName(), Variant.ofString("function"))
                        .tag(StackFrameTags.FILE_TAG.getName(), Variant.ofString("SomeModule.java"))
                        .tag(StackFrameTags.LINE_NUMBER_TAG.getName(), Variant.ofInteger(100))
                        .tag(StackFrameTags.COLUMN_NUMBER_TAG.getName(), Variant.ofShort((short) 12))
                        .build(),
                Container.builder()
                        .tag(StackFrameTags.TYPE_TAG.getName(), Variant.ofString("com.example.test.AnotherModule"))
                        .tag(StackFrameTags.FUNCTION_TAG.getName(), Variant.ofString("function"))
                        .tag(StackFrameTags.FILE_TAG.getName(), Variant.ofString("AnotherModule.java"))
                        .tag(StackFrameTags.LINE_NUMBER_TAG.getName(), Variant.ofInteger(200))
                        .tag(StackFrameTags.COLUMN_NUMBER_TAG.getName(), Variant.ofShort((short) 13))
                        .build()
        );
    }
}
