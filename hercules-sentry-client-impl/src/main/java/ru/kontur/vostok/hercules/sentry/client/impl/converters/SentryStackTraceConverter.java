package ru.kontur.vostok.hercules.sentry.client.impl.converters;

import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.util.ContainerUtil;
import ru.kontur.vostok.hercules.sentry.client.impl.client.v7.model.SentryStackFrame;
import ru.kontur.vostok.hercules.sentry.client.impl.client.v7.model.SentryStackTrace;
import ru.kontur.vostok.hercules.tags.StackFrameTags;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

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
        ArrayList<SentryStackFrame> frames = Arrays.stream(containers)
                .map(SentryStackFrameConverter::convert)
                .collect(Collectors.toCollection(() -> new ArrayList<>(containers.length)));
        return new SentryStackTrace()
                .setFrames(frames);
    }

    static class SentryStackFrameConverter {

        /**
         * @param container the container with values of the StackFrame tags from the Hercules event
         * @return the SentryStackFrame
         */
        public static SentryStackFrame convert(Container container) {
            SentryStackFrame stackFrame = new SentryStackFrame();
            stackFrame.setPackage(ContainerUtil.extract(container, StackFrameTags.TYPE_TAG).orElse(""));
            stackFrame.setFunction(ContainerUtil.extract(container, StackFrameTags.FUNCTION_TAG).orElse(""));
            String filename = ContainerUtil.extract(container, StackFrameTags.FILE_TAG).orElse("");
            stackFrame.setFilename(filename);
            stackFrame.setAbsPath(filename);
            stackFrame.setLineno(ContainerUtil.extract(container, StackFrameTags.LINE_NUMBER_TAG).orElse(0));
            stackFrame.setColno(ContainerUtil.extract(container, StackFrameTags.COLUMN_NUMBER_TAG).orElse(null));
            return stackFrame;
        }
    }
}
