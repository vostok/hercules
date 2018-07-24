package ru.kontur.vostok.hercules.logger.core;

import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Variant;

import java.util.HashMap;
import java.util.Map;

/**
 * Builder to construct log stack trace element
 *
 * @author Daniil Zhenikhov
 */
public class LogStackTraceElementBuilder {
    private final LogExceptionBuilder parent;
    private final Map<String, Variant> map;

    LogStackTraceElementBuilder(LogExceptionBuilder parent) {
        this.parent = parent;
        this.map = new HashMap<>();
    }

    public LogStackTraceElementBuilder setFile(String filename) {
        map.put("file", Variant.ofString(filename));
        return this;
    }

    public LogStackTraceElementBuilder setLine(int line) {
        map.put("line", Variant.ofInteger(line));
        return this;
    }

    public LogStackTraceElementBuilder setSource(String source) {
        map.put("source", Variant.ofString(source));
        return this;
    }

    public LogStackTraceElementBuilder setFunction(String function) {
        map.put("function", Variant.ofString(function));
        return this;
    }

    /**
     * build container of stack trace element exception and add one to LogException parent builder
     *
     * @return LogStackTraceElement parent builder
     */
    public LogExceptionBuilder endStackTraceElement() {
        requireFields();

        parent.addStackTraceElement(new Container(map));
        return parent;
    }

    private void requireFields() {
        if (!map.containsKey("file")
                || !map.containsKey("line")
                || !map.containsKey("source")
                || !map.containsKey("function")) {
            throw new IllegalStateException("Require all fields to build LogStackTraceElement");
        }
    }
}
