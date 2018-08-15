package ru.kontur.vostok.hercules.util.application.shutdown;

import ch.qos.logback.classic.LoggerContext;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * LogbackStopper
 *
 * @author Kirill Sulim
 */
public class LogbackStopper implements Stoppable {

    @Override
    public void stop(long timeout, TimeUnit timeUnit) {
        final LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        loggerContext.stop();
    }
}
