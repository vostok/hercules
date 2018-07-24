package ru.kontur.vostok.hercules.logger.log4j;

import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import ru.kontur.vostok.hercules.gateway.client.DefaultConfigurationConstants;
import ru.kontur.vostok.hercules.gateway.client.EventPublisher;
import ru.kontur.vostok.hercules.gateway.client.EventPublisherFactory;
import ru.kontur.vostok.hercules.logger.log4j.util.Log4jToEventConverter;

import java.io.Serializable;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Appender for log4j logger
 *
 * @author Daniil Zhenikhov
 */
@Plugin(name = "Log4jHttpAppender", category = "Core", elementType = "appender", printObject = true)
public class Log4jHttpAppender extends AbstractAppender {
    private EventPublisher publisher;
    private String queueName;

    private Log4jHttpAppender(String name,
                              Filter filter,
                              Layout<? extends Serializable> layout,

                              String stream,
                              String queueName,
                              boolean loseOnOverflow,
                              long periodMillis,
                              int batchSize,
                              int capacity) {
        super(name, filter, layout);

        this.queueName = queueName;

        publisher = EventPublisherFactory.getInstance();
        publisher.register(queueName, stream, periodMillis, capacity, batchSize, loseOnOverflow);
        publisher.start();
    }

    @PluginFactory
    public static Log4jHttpAppender createAppender(
            @PluginAttribute("name") String name,
            @PluginElement("Filter") final Filter filter,
            @PluginElement("Layout") Layout<? extends Serializable> layout,

            @PluginAttribute("stream") final String stream,
            @PluginAttribute("queueName") final String queueName,
            @PluginAttribute("loseOnOverflow") final Boolean loseOnOverflow,
            @PluginAttribute("periodMillis") final Long periodMillis,
            @PluginAttribute("batchSize") final Integer batchSize,
            @PluginAttribute("capacity") final  Integer capacity
    ) {
        if (stream == null) {
            LOGGER.error("stream is not chosen");
        }

        if (queueName == null) {
            LOGGER.error("queue's name is not chosen");
        }

        return new Log4jHttpAppender(
                name,
                filter,
                layout,

                stream,
                queueName,
                Objects.nonNull(loseOnOverflow) ? loseOnOverflow : DefaultConfigurationConstants.DEFAULT_IS_LOSE_ON_OVERFLOW,
                Objects.nonNull(periodMillis) ? periodMillis : DefaultConfigurationConstants.DEFAULT_PERIOD_MILLIS,
                Objects.nonNull(batchSize) ? batchSize : DefaultConfigurationConstants.DEFAULT_BATCH_SIZE,
                Objects.nonNull(capacity) ? capacity : DefaultConfigurationConstants.DEFAULT_CAPACITY
        );
    }

    @Override
    public void append(LogEvent logEvent) {
        publisher.publish(queueName, Log4jToEventConverter.createEvent(logEvent));

    }

    @Override
    public void stop() {
        super.stop();
        publisher.stop(1000);
    }

    @Override
    public boolean stop(long timeout, TimeUnit timeUnit) {
        boolean stopped = super.stop(timeout, timeUnit);
        publisher.stop(timeUnit.toMillis(timeout) == 0 ? 1000 : timeUnit.toMillis(timeout));
        return stopped;
    }
}
