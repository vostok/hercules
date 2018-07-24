package ru.kontur.vostok.hercules.logger.core;

import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.protocol.encoder.EventBuilder;
import ru.kontur.vostok.hercules.uuid.UuidGenerator;

import java.util.ArrayList;
import java.util.List;

/**
 * Builder to construct event of log
 *
 * @author Daniil Zhenikhov
 */
public class LogEventBuilder {
    private static final UuidGenerator GENERATOR = UuidGenerator.getClientInstance();

    private final EventBuilder eventBuilder;
    private final List<Container> exceptionContainers;

    private boolean levelSet = false;
    private boolean messageSet = false;

    public LogEventBuilder() {
        eventBuilder = new EventBuilder();
        eventBuilder.setVersion(1);
        eventBuilder.setEventId(GENERATOR.next());

        exceptionContainers = new ArrayList<>();
    }

    public LogEventBuilder setLevel(String level) {
        eventBuilder.setTag("level", Variant.ofString(level));
        levelSet = true;
        return this;
    }

    public LogEventBuilder setMessage(String message) {
        eventBuilder.setTag("message", Variant.ofString(message));
        messageSet = true;
        return this;
    }

    public LogEventBuilder setProperty(String key, Variant variant) {
        eventBuilder.setTag(key, variant);
        return this;
    }

    /**
     * Define builder for log exception
     *
     * @return new instance of that builder
     */
    public LogExceptionBuilder withException() {
        return new LogExceptionBuilder(this);
    }

    public Event build() {
        requireFields();

        if (!exceptionContainers.isEmpty()) {
            eventBuilder.setTag("exceptions", Variant.ofContainerArray(exceptionContainers.toArray(new Container[exceptionContainers.size()])));
        }

        return eventBuilder.build();
    }

    void addException(Container container) {
        exceptionContainers.add(container);
    }

    private void requireFields() {
        if (!levelSet || !messageSet) {
            throw new IllegalStateException("Require all fields to build LogEvent");
        }
    }
}
