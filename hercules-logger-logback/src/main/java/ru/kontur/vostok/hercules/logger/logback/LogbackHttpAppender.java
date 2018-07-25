package ru.kontur.vostok.hercules.logger.logback;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import ru.kontur.vostok.hercules.gateway.client.DefaultConfigurationConstants;
import ru.kontur.vostok.hercules.gateway.client.EventPublisher;
import ru.kontur.vostok.hercules.gateway.client.EventPublisherFactory;
import ru.kontur.vostok.hercules.logger.core.util.LogCoreUtil;
import ru.kontur.vostok.hercules.logger.logback.util.LogbackToEventConverter;

import java.util.Objects;
import java.util.Optional;

/**
 * Appender for logback logger
 *
 * @author Daniil Zhenikhov
 */
public class LogbackHttpAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {
    private EventPublisher publisher = EventPublisherFactory.getInstance();
    private String stream;
    private String queueName;
    private Integer batchSize;
    private Integer capacity;
    private Long periodMillis;
    private Boolean loseOnOverflow;

    @Override
    public void start() {
        publisher.register(
                getQueueName(),
                getStream(),
                getPeriodMillis(),
                getCapacity(),
                getBatchSize(),
                getLoseOnOverflow()
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
        publisher.publish(queueName, LogbackToEventConverter.createEvent(event));
    }

    public String getStream() {
        return Optional
                .ofNullable(stream)
                .orElseThrow(LogCoreUtil.missingLoggerConfiguration("stream"));
    }

    public void setStream(String stream) {
        this.stream = stream;
    }

    public String getQueueName() {
        return Optional
                .ofNullable(queueName)
                .orElseThrow(LogCoreUtil.missingLoggerConfiguration("queue name"));
    }

    public void setQueueName(String queueName) {
        this.queueName = queueName;
    }

    public Integer getBatchSize() {
        return Objects.nonNull(batchSize)
                ? batchSize
                : DefaultConfigurationConstants.DEFAULT_BATCH_SIZE;
    }

    public void setBatchSize(Integer batchSize) {
        this.batchSize = batchSize;
    }

    public Integer getCapacity() {
        return Objects.nonNull(capacity)
                ? capacity
                : DefaultConfigurationConstants.DEFAULT_CAPACITY;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public Long getPeriodMillis() {
        return Objects.nonNull(periodMillis)
                ? periodMillis
                : DefaultConfigurationConstants.DEFAULT_PERIOD_MILLIS;
    }

    public void setPeriodMillis(Long periodMillis) {
        this.periodMillis = periodMillis;
    }

    public Boolean getLoseOnOverflow() {
        return Objects.nonNull(loseOnOverflow)
                ? loseOnOverflow
                : DefaultConfigurationConstants.DEFAULT_IS_LOSE_ON_OVERFLOW;
    }

    public void setLoseOnOverflow(Boolean loseOnOverflow) {
        this.loseOnOverflow = loseOnOverflow;
    }
}

