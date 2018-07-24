package ru.kontur.vostok.hercules.sentry.sink;

import io.sentry.DefaultSentryClientFactory;
import io.sentry.Sentry;
import io.sentry.SentryClient;
import io.sentry.SentryClientFactory;
import org.apache.kafka.streams.processor.AbstractProcessor;
import ru.kontur.vostok.hercules.protocol.Event;

import java.util.Properties;
import java.util.UUID;

/**
 * @author Gregory Koshelev
 */
public class SentrySyncProcessor extends AbstractProcessor<UUID, Event> {

    private static final String DISABLE_UNCAUGHT_EXCEPTION_HANDLING = DefaultSentryClientFactory.UNCAUGHT_HANDLER_ENABLED_OPTION + "=false";

    private final SentryClient sentryClient;


    public SentrySyncProcessor(Properties properties) {
        String dsn = properties.getProperty("dsn") + "&" + DISABLE_UNCAUGHT_EXCEPTION_HANDLING;


        this.sentryClient = SentryClientFactory.sentryClient(dsn);
    }

    @Override
    public void process(UUID key, Event value) {
        sentryClient.sendEvent(SentryEventConverter.convert(value));
    }
}
