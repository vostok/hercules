package ru.kontur.vostok.hercules.sentry.sink;

import io.sentry.SentryClient;
import org.apache.kafka.streams.processor.AbstractProcessor;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.Type;
import ru.kontur.vostok.hercules.protocol.util.ContainerUtil;
import ru.kontur.vostok.hercules.protocol.util.FieldDescription;
import ru.kontur.vostok.hercules.sentry.sink.converters.SentryEventConverter;

import java.util.Optional;
import java.util.UUID;

/**
 * @author Gregory Koshelev
 */
public class SentrySyncProcessor extends AbstractProcessor<UUID, Event> {

    public static final FieldDescription SENTRY_PROJECT_NAME_TAG = FieldDescription.create("sentry-project-name", Type.STRING);

    private final SentryClientHolder sentryClientHolder;

    public SentrySyncProcessor(SentryClientHolder sentryClientHolder) {
        this.sentryClientHolder = sentryClientHolder;
    }

    @Override
    public void process(UUID key, Event value) {
        Optional<String> token = ContainerUtil.extractOptional(value.getPayload(), SENTRY_PROJECT_NAME_TAG);
        if (!token.isPresent()) {
            // TODO: logging
            System.out.println("Missing required tag '" + SENTRY_PROJECT_NAME_TAG.getName() + "'");
            return;
        }

        Optional<SentryClient> sentryClient = sentryClientHolder.getClient(token.get());
        if (!sentryClient.isPresent()) {
            System.out.println("Missing client for project '" + token.get() + "'");
            return;
        }

        sentryClient.get().sendEvent(SentryEventConverter.convert(value));
    }
}
