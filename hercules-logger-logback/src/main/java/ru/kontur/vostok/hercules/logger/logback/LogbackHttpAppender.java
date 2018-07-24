package ru.kontur.vostok.hercules.logger.logback;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import ru.kontur.vostok.hercules.gateway.client.ConfigurationConstants;
import ru.kontur.vostok.hercules.gateway.client.EventPublisher;
import ru.kontur.vostok.hercules.gateway.client.EventPublisherFactory;
import ru.kontur.vostok.hercules.logger.logback.util.LogbackToEventConverter;

import java.util.Objects;

/**
 * Appender for logback logger
 *
 * @author Daniil Zhenikhov
 */
public class LogbackHttpAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {
    private EventPublisher publisher = EventPublisherFactory.create();
    private LogbackHttpConfiguration configuration;

    @Override
    public void start() {
        checkForNull();

        publisher.register(
                configuration.getName(),
                configuration.getStream(),
                Objects.nonNull(configuration.getPeriodMillis())
                        ? configuration.getPeriodMillis()
                        : ConfigurationConstants.DEFAULT_PERIOD_MILLIS,
                Objects.nonNull(configuration.getCapacity())
                        ? configuration.getCapacity()
                        : ConfigurationConstants.DEFAULT_CAPACITY,
                Objects.nonNull(configuration.getBatchSize())
                        ? configuration.getBatchSize()
                        : ConfigurationConstants.DEFAULT_BATCH_SIZE,
                Objects.nonNull(configuration.getLoseOnOverflow())
                        ? configuration.getLoseOnOverflow()
                        : ConfigurationConstants.DEFAULT_IS_LOSE_ON_OVERFLOW
        );

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
        publisher.publish(configuration.getName(), LogbackToEventConverter.createEvent(event));
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

        if (configuration.getName() == null) {
            throw new IllegalStateException("queue's name is empty");
        }

    }
}

