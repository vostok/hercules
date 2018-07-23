package ru.kontur.vostok.hercules.logger.logback;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import ru.kontur.vostok.hercules.gateway.client.EventPublisher;
import ru.kontur.vostok.hercules.gateway.client.EventPublisherFactory;
import ru.kontur.vostok.hercules.logger.logback.util.LogbackToEventConverter;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ThreadFactory;

/**
 * Appender for logback logger
 *
 * @author Daniil Zhenikhov
 */
public class LogbackHttpAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {
    private EventPublisher publisher;
    private LogbackHttpConfiguration configuration;

    private Set<Thread> threads = new HashSet<>();
    private EventPublisherFactory publisherFactory = new EventPublisherFactory();
    private ThreadFactory threadFactory = r -> {
        Thread thread = new Thread(r);
        thread.setDaemon(true);

        String key = String.valueOf(thread.getId());
        thread.setName(key);
        threads.add(thread);

        return thread;
    };

    @Override
    public void start() {
        checkForNull();

        publisher = publisherFactory.create(
                configuration.getStream(),
                configuration.getLoseOnOverflow(),
                threadFactory);
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
        if (configuration.getStream() == null) {
            throw new IllegalStateException("Stream is empty");
        }

        if (configuration.getLoseOnOverflow() == null) {
            throw new IllegalStateException("LoseOnOverflow is empty");
        }

    }
}

