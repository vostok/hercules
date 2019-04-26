package ru.kontur.vostok.hercules.sentry.sink;

import io.sentry.SentryClient;
import io.sentry.event.Event.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.kafka.util.processing.BackendServiceFailedException;
import ru.kontur.vostok.hercules.kafka.util.processing.single.SingleSender;
import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.util.ContainerUtil;
import ru.kontur.vostok.hercules.sentry.sink.converters.SentryEventConverter;
import ru.kontur.vostok.hercules.sentry.sink.converters.SentryLevelEnumParser;
import ru.kontur.vostok.hercules.tags.CommonTags;
import ru.kontur.vostok.hercules.tags.LogEventTags;
import ru.kontur.vostok.hercules.util.properties.PropertyDescription;
import ru.kontur.vostok.hercules.util.properties.PropertyDescriptions;

import java.util.Optional;
import java.util.Properties;
import java.util.UUID;

/**
 * @author Gregory Koshelev
 */
public class SentrySyncProcessor implements SingleSender<UUID, Event> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SentrySyncProcessor.class);

    private final Level requiredLevel;
    private final String defaultSentryProject;
    private final SentryClientHolder sentryClientHolder;

    public SentrySyncProcessor(
            Properties sinkProperties,
            SentryClientHolder sentryClientHolder
    ) {
        this.requiredLevel = Props.REQUIRED_LEVEL.extract(sinkProperties);
        this.defaultSentryProject = Props.DEFAULT_PROJECT.extract(sinkProperties);
        this.sentryClientHolder = sentryClientHolder;
    }

    @Override
    public boolean process(UUID key, Event event) throws BackendServiceFailedException {
        final Optional<Level> level = ContainerUtil.extract(event.getPayload(), LogEventTags.LEVEL_TAG)
                .flatMap(SentryLevelEnumParser::parse);
        if (!level.isPresent() || requiredLevel.compareTo(level.get()) < 0) {
            return false;
        }

        final Optional<Container> properties = ContainerUtil.extract(event.getPayload(), CommonTags.PROPERTIES_TAG);
        if (!properties.isPresent()) {
            LOGGER.warn("Missing required tag '{}'", CommonTags.PROPERTIES_TAG.getName());
            return false;
        }

        Optional<String> organizationName = ContainerUtil.extract(properties.get(), CommonTags.PROJECT_TAG);
        if (!organizationName.isPresent()) {
            LOGGER.warn("Missing required tag '{}'", CommonTags.PROJECT_TAG.getName());
            return false;
        }

        Optional<String> sentryProjectName = ContainerUtil.extract(properties.get(), CommonTags.SCOPE_TAG);
        if (!sentryProjectName.isPresent()) {
            sentryProjectName = Optional.of(defaultSentryProject);
        }

        sentryClientHolder.update();
        Optional<SentryClient> sentryClient = sentryClientHolder.getOrCreateClient(organizationName.get(), sentryProjectName.get());
        if (!sentryClient.isPresent()) {
            LOGGER.error(String.format("Cannot get client for Sentry organization/project '{%s/%s}'",
                    organizationName.get(), sentryProjectName.get()));
            return false;
        }

        try {
            io.sentry.event.Event sentryEvent = SentryEventConverter.convert(event);
            sentryClient.get().sendEvent(sentryEvent);
            return true;
        } catch (Exception e) {
            //FIXME If a project (or DSN, ...) is removed from Sentry but exists in cache, the sentryClient will contain removed data.
            //FIXME The event will not be received by Sentry with this sentryClient.
            //FIXME We need to update cache and retry
            throw new BackendServiceFailedException(e);
        }
    }

    @Override
    public void close() throws Exception {
    }

    private static class Props {
        static final PropertyDescription<Level> REQUIRED_LEVEL = PropertyDescriptions
                .propertyOfType(Level.class, "sentry.level")
                .withParser(SentryLevelEnumParser::parseAsResult)
                .withDefaultValue(Level.WARNING)
                .build();
        static final PropertyDescription<String> DEFAULT_PROJECT = PropertyDescriptions
                .stringProperty("sentry.default.project")
                .withDefaultValue("default_project")
                .build();
    }
}
