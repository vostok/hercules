package ru.kontur.vostok.hercules.sentry.sink;

import io.sentry.SentryClient;
import io.sentry.connection.ConnectionException;
import io.sentry.connection.LockedDownException;
import io.sentry.dsn.InvalidDsnException;
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
import ru.kontur.vostok.hercules.util.functional.Result;
import ru.kontur.vostok.hercules.util.properties.PropertyDescription;
import ru.kontur.vostok.hercules.util.properties.PropertyDescriptions;

import java.util.Optional;
import java.util.Properties;
import java.util.UUID;

import static io.sentry.connection.LockdownManager.DEFAULT_BASE_LOCKDOWN_TIME;

/**
 * @author Gregory Koshelev
 */
public class SentrySyncProcessor implements SingleSender<UUID, Event> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SentrySyncProcessor.class);

    private final Level requiredLevel;
    private final SentryClientHolder sentryClientHolder;

    public SentrySyncProcessor(
            Properties sinkProperties,
            SentryClientHolder sentryClientHolder
    ) {
        this.requiredLevel = Props.REQUIRED_LEVEL.extract(sinkProperties);
        this.sentryClientHolder = sentryClientHolder;
        this.sentryClientHolder.update();
    }

    /**
     * Process event
     *
     * @param key UUID of event
     * @param event event
     * @return true if event is successfully processed or false otherwise
     * @throws BackendServiceFailedException
     */
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
        String organization = organizationName.get();

        Optional<String> sentryProjectName = ContainerUtil.extract(properties.get(), CommonTags.APPLICATION_TAG);
        String sentryProject = sentryProjectName.orElse(organization);

        int retryCount = 3; //FIXME hard code
        do {
            SentrySinkError processError = null;
            Result<SentryClient, SentrySinkError> sentryClient =
                    sentryClientHolder.getOrCreateClient(organization, sentryProject);
            if (!sentryClient.isOk()) {
                LOGGER.error(String.format("Cannot get client for Sentry organization/project '%s/%s'",
                        organization, sentryProject));
                processError = sentryClient.getError();
            } else {
                try {
                    io.sentry.event.Event sentryEvent = SentryEventConverter.convert(event);
                    sentryClient.get().sendEvent(sentryEvent);
                } catch (InvalidDsnException e) {
                    LOGGER.error("InvalidDsnException: " + e.getMessage());
                    processError = new SentrySinkError(false);
                } catch (LockedDownException e) {
                    LOGGER.error("LockedDownException: a temporary lockdown is switched on");
                    processError = new SentrySinkError(true, DEFAULT_BASE_LOCKDOWN_TIME);
                } catch (ConnectionException e) {
                    LOGGER.error(String.format("ConnectionException: %d %s", e.getResponseCode(), e.getMessage()));
                    if (e.getRecommendedLockdownTime() != null) {
                        processError = new SentrySinkError(e.getResponseCode(), e.getRecommendedLockdownTime());
                    } else {
                        processError = new SentrySinkError(e.getResponseCode());
                    }

                } catch (Exception e) {
                    throw new BackendServiceFailedException(e);
                }
            }
            if (processError == null) {
                return true;

            } else if (processError.isRetryable()) {
                if (processError.needToUpdate()) {
                    sentryClientHolder.update();
                }
                long waitingTimeMs = processError.getWaitingTimeMs();
                if (waitingTimeMs > 0) {
                    try {
                        Thread.sleep(waitingTimeMs);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                return false;
            }
         } while (0 < retryCount--);

        return false;
    }

    @Override
    public void close() {
    }

    private static class Props {
        static final PropertyDescription<Level> REQUIRED_LEVEL = PropertyDescriptions
                .propertyOfType(Level.class, "sentry.level")
                .withParser(SentryLevelEnumParser::parseAsResult)
                .withDefaultValue(Level.WARNING)
                .build();
    }
}
