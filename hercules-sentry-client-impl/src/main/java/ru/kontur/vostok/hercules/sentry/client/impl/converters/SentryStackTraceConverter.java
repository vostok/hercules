package ru.kontur.vostok.hercules.sentry.client.impl.converters;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;
import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.util.ContainerUtil;
import ru.kontur.vostok.hercules.sentry.client.impl.client.v7.model.SentryStackFrame;
import ru.kontur.vostok.hercules.sentry.client.impl.client.v7.model.SentryStackTrace;
import ru.kontur.vostok.hercules.tags.StackFrameTags;

/**
 * Allows convert exception stack trace to Sentry StackTrace interface
 *
 * @author Tatyana Tokmyanina
 */
public class SentryStackTraceConverter {
    /**
     * @param containers array of containers with values of the StackFrame tag from the Hercules event
     * @return SentryStackTrace - list of SentryStackFrames
     */
    public static SentryStackTrace convert(Container[] containers) {
        return new SentryStackTrace()
                .setFrames(Arrays.stream(containers)
                .map(SentryStackFrameConverter::convert)
                .collect(Collectors.toCollection(() -> new ArrayList<>(containers.length))));
    }

    static class SentryStackFrameConverter {
        /**
         * @param container the container with values of the StackFrame tags from the Hercules event
         * @return the SentryStackFrame
         */
        public static SentryStackFrame convert(Container container) {
            SentryStackFrame stackFrame = new SentryStackFrame();
            ContainerUtil.extract(container, StackFrameTags.FUNCTION_TAG)
                    .ifPresent(stackFrame::setFunction);
            ContainerUtil.extract(container, StackFrameTags.TYPE_TAG)
                    .ifPresent(stackFrame::setPackage);
            ContainerUtil.extract(container, StackFrameTags.FILE_TAG)
                    .ifPresent(stackFrame::setFilename);
            ContainerUtil.extract(container, StackFrameTags.LINE_NUMBER_TAG)
                    .ifPresent(stackFrame::setLineno);
            ContainerUtil.extract(container, StackFrameTags.COLUMN_NUMBER_TAG)
                    .ifPresent(stackFrame::setColno);
            return stackFrame;
        }
    }
}
