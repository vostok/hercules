package ru.kontur.vostok.hercules.elastic.sink;

import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.util.ContainerUtil;
import ru.kontur.vostok.hercules.tags.ExceptionTags;
import ru.kontur.vostok.hercules.tags.StackFrameTags;

/**
 * @author Vladimir Tsypaev
 */
public final class StackTraceCreator {

    public static String createStackTrace(Container exception) {
        return createStackTrace(exception, "");
    }

    private static String createStackTrace(Container exception, String indent) {
        StringBuilder stackTrace = new StringBuilder();

        writeExceptionInfo(stackTrace, exception);

        ContainerUtil.extract(exception, ExceptionTags.STACK_FRAMES)
                .ifPresent(stackFrames -> writeStackFrames(indent, stackTrace, stackFrames));

        ContainerUtil.extract(exception, ExceptionTags.INNER_EXCEPTIONS_TAG)
                .ifPresent(innerExceptions -> writeInnerExceptions(indent, stackTrace, innerExceptions));

        return stackTrace.toString();
    }

    private static void writeExceptionInfo(StringBuilder stackTrace, Container exception) {
        ContainerUtil.extract(exception, ExceptionTags.TYPE_TAG)
                .ifPresent(s -> stackTrace.append(s).append(": "));

        ContainerUtil.extract(exception, ExceptionTags.MESSAGE_TAG)
                .ifPresent(stackTrace::append);
    }

    private static void writeStackFrames(String indent, StringBuilder stackTrace, Container[] stackFrames) {
        for (Container stackFrame : stackFrames) {
            ContainerUtil.extract(stackFrame, StackFrameTags.TYPE_TAG)
                    .ifPresent(type -> stackTrace.append("\n").append(indent).append("    ").append("at ").append(type));

            ContainerUtil.extract(stackFrame, StackFrameTags.FUNCTION_TAG)
                    .ifPresent(function -> stackTrace.append(".").append(function));

            stackTrace.append("(");

            ContainerUtil.extract(stackFrame, StackFrameTags.FILE_TAG)
                    .ifPresent(stackTrace::append);

            ContainerUtil.extract(stackFrame, StackFrameTags.LINE_NUMBER_TAG)
                    .ifPresent(line -> stackTrace.append(":").append(line));

            ContainerUtil.extract(stackFrame, StackFrameTags.COLUMN_NUMBER_TAG)
                    .ifPresent(column -> stackTrace.append(":").append(column));

            stackTrace.append(")");
        }
    }

    private static void writeInnerExceptions(String indent, StringBuilder stackTrace, Container[] innerExceptions) {
        for (Container container : innerExceptions) {
            String levelIndent = indent + "  ";
            stackTrace.append("\n").append(levelIndent).append("Caused by: ");
            stackTrace.append(createStackTrace(container, levelIndent));
        }
    }

    public StackTraceCreator() {
        /* static class */
    }
}
