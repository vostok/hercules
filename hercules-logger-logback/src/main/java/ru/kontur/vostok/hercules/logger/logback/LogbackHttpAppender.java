package ru.kontur.vostok.hercules.logger.logback;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import ru.kontur.vostok.hercules.gateway.client.EventPublisher;
import ru.kontur.vostok.hercules.logger.logback.util.LogbackToEventConverter;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ThreadFactory;

public class LogbackHttpAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {
    private LogbackHttpConfiguration configuration;
    private Set<Thread> threads;
    private EventPublisher publisher;

    @Override
    public void start() {
        checkForNull();
        threads = new HashSet<>();

        ThreadFactory threadFactory = r -> {
            Thread thread = new Thread(r);
            thread.setDaemon(true);

            String key = String.valueOf(thread.getId());
            thread.setName(key);
            threads.add(thread);

            return thread;
        };

        publisher = new EventPublisher(
                configuration.getStream(),
                configuration.getPeriodMillis(),
                configuration.getBatchSize(),
                configuration.getThreads(),
                configuration.getCapacity(),
                configuration.getLoseOnOverflow(),
                threadFactory,
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
        if (!threads.contains(Thread.currentThread())) {
            publisher.publish(LogbackToEventConverter.createEvent(event));
        }
    }

    public LogbackHttpConfiguration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(LogbackHttpConfiguration configuration) {
        this.configuration = configuration;
    }

    private void checkForNull() {
        if (configuration.getUrl() == null) {
            throw new IllegalStateException("Url is empty");
        }
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
        if (configuration.getPeriodMillis() == null) {
            throw new IllegalStateException("PeriodMillis is empty");
        }
        if (configuration.getThreads() == null) {
            throw new IllegalStateException("Threads is empty");
        }

    }
}

