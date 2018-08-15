package ru.kontur.vostok.hercules.util.application.shutdown;

import java.util.concurrent.TimeUnit;

/**
 * Stoppable - interface for components which are needed to be carefully stopped
 *
 * @author Kirill Sulim
 */
public interface Stoppable {

    void stop(long timeout, TimeUnit timeUnit);
}
