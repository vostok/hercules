package ru.kontur.vostok.hercules.logger.log4j;

import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import ru.kontur.vostok.hercules.gateway.client.EventPublisher;
import ru.kontur.vostok.hercules.logger.log4j.util.Log4jToEventConverter;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

@Plugin(name = "Log4jHttpAppender", category = "Core", elementType = "appender", printObject = true)
public class Log4jHttpAppender extends AbstractAppender {
    private EventPublisher publisher;
    private Set<Thread> threadSet = new HashSet<>();

    private Log4jHttpAppender(String name,
                              Filter filter,
                              Layout<? extends Serializable> layout,

                              String url,
                              String apiKey,
                              String stream,
                              int batchSize,
                              int capacity,
                              long periodMillis,
                              int threads,
                              boolean loseOnOverflow) {
        super(name, filter, layout);

        ThreadFactory threadFactory = r -> {
            Thread thread = Executors.defaultThreadFactory().newThread(r);
            thread.setDaemon(true);
            threadSet.add(thread);
            return thread;
        };

        publisher = new EventPublisher(
                stream,
                periodMillis,
                batchSize,
                threads,
                capacity,
                loseOnOverflow,
                threadFactory,
                url,
                apiKey);
        publisher.start();
    }

    @PluginFactory
    public static Log4jHttpAppender createAppender(
            @PluginAttribute("name") String name,
            @PluginElement("Filter") final Filter filter,
            @PluginElement("Layout") Layout<? extends Serializable> layout,

            @PluginAttribute("url") final String url,
            @PluginAttribute("apiKey") final String apiKey,
            @PluginAttribute("stream") final String stream,
            @PluginAttribute("batchSize") final int batchSize,
            @PluginAttribute("capacity") final int capacity,
            @PluginAttribute("periodMillis") final long periodMillis,
            @PluginAttribute("threads") final int threads,
            @PluginAttribute("loseOnOverflow") final boolean loseOnOverflow
    ) {
        if (url == null) {
            LOGGER.error("Url is undefined");
            return null;
        }

        if (apiKey == null) {
            LOGGER.error("apiKey is undefined");
            return null;
        }

        if (stream == null) {
            LOGGER.error("stream is not chosen");
        }

        return new Log4jHttpAppender(
                name,
                filter,
                layout,

                url,
                apiKey,
                stream,
                batchSize,
                capacity,
                periodMillis,
                threads,
                loseOnOverflow
        );
    }

    @Override
    public void append(LogEvent logEvent) {
        if (!threadSet.contains(Thread.currentThread())) {
            publisher.publish(Log4jToEventConverter.createEvent(logEvent));
        }
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
