package ru.kontur.vostok.hercules.sentry.sink;

import io.sentry.SentryClient;
import org.apache.kafka.streams.processor.AbstractProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.Type;
import ru.kontur.vostok.hercules.protocol.util.ContainerUtil;
import ru.kontur.vostok.hercules.protocol.util.TagDescription;
import ru.kontur.vostok.hercules.sentry.sink.converters.SentryEventConverter;
import ru.kontur.vostok.hercules.tags.CommonTags;

import java.util.Optional;
import java.util.UUID;

/**
 * @author Gregory Koshelev
 */
public class SentrySyncProcessor extends AbstractProcessor<UUID, Event> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SentrySyncProcessor.class);

    private final SentryClientHolder sentryClientHolder;
    private final SentryProjectRegistry sentryProjectRegistry;

    public SentrySyncProcessor(
            SentryClientHolder sentryClientHolder,
            SentryProjectRegistry sentryProjectRegistry
    ) {
        this.sentryClientHolder = sentryClientHolder;
        this.sentryProjectRegistry = sentryProjectRegistry;
    }

    @Override
    public void process(UUID key, Event value) {
        Optional<String> project = ContainerUtil.extract(value.getPayload(), CommonTags.PROJECT_TAG);
        if (!project.isPresent()) {
            LOGGER.warn("Missing required tag '{}'", CommonTags.PROJECT_TAG.getName());
            return;
        }

        Optional<String> sentryProjectName = sentryProjectRegistry.getSentryProjectName(project.get());
        if (!sentryProjectName.isPresent()) {
            LOGGER.warn("Project '{}' not found in registry", project.get());
            return;
        }

        Optional<SentryClient> sentryClient = sentryClientHolder.getClient(sentryProjectName.get());
        if (!sentryClient.isPresent()) {
            LOGGER.warn("Missing client for project '{}'", project.get());
            return;
        }

        try {
            io.sentry.event.Event sentryEvent = SentryEventConverter.convert(value);
            sentryClient.get().sendEvent(sentryEvent);
        }
        catch (Exception e) {
            LOGGER.error("Exception while trying to process event", e);
        }
    }
}
