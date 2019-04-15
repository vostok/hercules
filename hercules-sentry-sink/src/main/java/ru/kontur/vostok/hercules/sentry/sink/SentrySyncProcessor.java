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
import ru.kontur.vostok.hercules.tags.ScopeTags;
import ru.kontur.vostok.hercules.util.properties.PropertyDescription;
import ru.kontur.vostok.hercules.util.properties.PropertyDescriptions;

import java.util.Optional;
import java.util.Properties;
import java.util.UUID;

/**
 * @author Gregory Koshelev
 */
public class SentrySyncProcessor implements SingleSender<UUID, Event> {

    private static class Props {
        static final PropertyDescription<Level> REQUIRED_LEVEL = PropertyDescriptions
                .propertyOfType(Level.class, "sentry.level")
                .withParser(SentryLevelEnumParser::parseAsResult)
                .withDefaultValue(Level.WARNING)
                .build();
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(SentrySyncProcessor.class);

    private static final String DEFAULT_SENTRY_PROJECT = "default";
    private static final String DEFAULT_ORGANISATION = "default";

    private final Level requiredLevel;
    private final SentryClientHolder sentryClientHolder;

    public SentrySyncProcessor(
            Properties properties,
            SentryClientHolder sentryClientHolder
    ) {
        this.requiredLevel = Props.REQUIRED_LEVEL.extract(properties);
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

        Optional<String> organisationName = ContainerUtil.extract(properties.get(), CommonTags.PROJECT_TAG);
        if (!organisationName.isPresent()) {
            organisationName = Optional.of(DEFAULT_ORGANISATION);
            /*LOGGER.warn("Missing required tag '{}'", CommonTags.PROJECT_TAG.getName());
            return false;*/
        }

        Optional<String> sentryProjectName = ContainerUtil.extract(properties.get(), ScopeTags.SCOPE_TAG);
        if (!sentryProjectName.isPresent()) {
            sentryProjectName = Optional.of(DEFAULT_SENTRY_PROJECT);
        }
        //Optional<String> service = ContainerUtil.extract(properties.get(), CommonTags.SERVICE_TAG);

/*        Optional<String> sentryProjectName = sentryProjectRegistry.getSentryProjectName(project.get(), service.orElse(null));
        if (!sentryProjectName.isPresent()) {
            LOGGER.warn("Project '{}' not found in registry", project.get());
            return false;
        }*/

        final String orgAndProjectPair = String.format("%s/%s", organisationName.get(), sentryProjectName.get());
        Optional<SentryClient> sentryClient = sentryClientHolder.getClient(orgAndProjectPair);
        if (!sentryClient.isPresent()) {
            //TODO if client is not present, it must be created in sentryClientHolder
            //TODO It means this if-block is not needed
            LOGGER.warn("Missing client for Sentry project '{}'", sentryProjectName.get());
            return false;
        }

        try {
            io.sentry.event.Event sentryEvent = SentryEventConverter.convert(event);
            sentryClient.get().sendEvent(sentryEvent);
            return true;
        } catch (Exception e) {
            throw new BackendServiceFailedException(e);
        }
    }

    @Override
    public void close() throws Exception {
    }
}
