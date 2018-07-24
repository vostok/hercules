package ru.kontur.vostok.hercules.logger.logback;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import ru.kontur.vostok.hercules.gateway.client.EventPublisher;
import ru.kontur.vostok.hercules.gateway.client.EventPublisherFactory;
import ru.kontur.vostok.hercules.gateway.client.EventQueue;
import ru.kontur.vostok.hercules.logger.logback.util.LogbackToEventConverter;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ThreadFactory;

/**
 * Appender for logback logger
 *
 * @author Daniil Zhenikhov
 */
public class LogbackHttpAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {
    private static final String QUEUE_NAME = "main";
    private EventPublisher publisher;
    private LogbackHttpConfiguration configuration;

    private Set<Thread> threads = new HashSet<>();
    private EventPublisherFactory publisherFactory = new EventPublisherFactory();
    private ThreadFactory threadFactory = r -> {

    @Override
                configuration.getThreads(),
                configuration.getLoseOnOverflow(),
                Collections.singletonList(new EventQueue(QUEUE_NAME,        publisher = publisherFactory.create(
                        configuration.getStream(),
                        configuration.getPeriodMillis(),
                        configuration.getCapacity(),
                        configuration.getBatchSize(),
                        configuration.getLoseOnOverflow())),
                configuration.getUrl(),
                configuration.getApiKey());
        publisher.start();
        super.start();
    }

    @Override
    public void stop() {
        super.stop();
        publisher.stop(1000);
    }

    @Override
    protected void append(ILoggingEvent event) {
        publisher.publish(QUEUE_NAME, LogbackToEventConverter.createEvent(event));
    }

    public LogbackHttpConfiguration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(LogbackHttpConfiguration configuration) {
        this.configuration = configuration;
    }

    private void checkForNull() {
        if (configuration.getStream() == null) {
            throw new IllegalStateException("Stream is empty");
        }

        if (configuration.getApiKey() == null) {
            throw new IllegalStateException("ApiKey is empty");
        }
        if (configuration.getBatchSize() == null) {
            throw new IllegalStateException("BatchSize is empty");
        }
        if (configuration.getCapacity() == null) {
            throw new IllegalStateException("Capacity is empty");
        }
        if (configuration.getLoseOnOverflow() == null) {
            throw new IllegalStateException("LoseOnOverflow is empty");
        }

    }
}

