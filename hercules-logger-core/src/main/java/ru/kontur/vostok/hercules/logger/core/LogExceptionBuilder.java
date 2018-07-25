package ru.kontur.vostok.hercules.logger.core;

import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Variant;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Builder to construct exception of log
 *
 * @author Daniil Zhenikhov
 */
public class LogExceptionBuilder {
    private static final int DEFAULT_CAPACITY = 100;

    private final LogEventBuilder parent;
    private final Map<String, Variant> map;
    private final List<Container> stackTraceElementContainers;

    LogExceptionBuilder(LogEventBuilder parent) {
        this.parent = parent;
        this.map = new HashMap<>();
        stackTraceElementContainers = new ArrayList<>(DEFAULT_CAPACITY);
    }

    public LogExceptionBuilder setType(String type) {
        map.put("type", Variant.ofString(type));
        return this;
    }

    public LogExceptionBuilder setMessage(String message) {
        map.put("message", Variant.ofString(message));
        return this;
    }

    public LogExceptionBuilder setModule(String module) {
        map.put("module", Variant.ofString(module));
        return this;
    }

    /**
     * Define stack trace element builder.
     *
     * @return instance of that builder
     */
    public LogStackTraceElementBuilder startStackTraceElement() {
        return new LogStackTraceElementBuilder(this);
    }

    /**
     * build container of log exception and add one to LogEvent parent builder
     *
     * @return LogEvent parent builder
     */
    public LogEventBuilder endException() {
        requireFields();

        if (!stackTraceElementContainers.isEmpty()) {
            map.put("stacktrace", Variant.ofContainerArray(stackTraceElementContainers.toArray(new Container[stackTraceElementContainers.size()])));
        }

        parent.addException(new Container(map));
        return parent;
    }

    void addStackTraceElement(Container container) {
        stackTraceElementContainers.add(container);
    }

    private void requireFields() {
        if (!map.containsKey("type")
                || !map.containsKey("message")
                || !map.containsKey("module")) {
            throw new IllegalStateException("Require all fields to build LogException");
        }
    }
}
