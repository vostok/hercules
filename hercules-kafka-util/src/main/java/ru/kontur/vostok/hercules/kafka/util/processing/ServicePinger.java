package ru.kontur.vostok.hercules.kafka.util.processing;

/**
 * ServicePinger - ping interface
 *
 * @author Kirill Sulim
 */
public interface ServicePinger {

    /**
     * Ping method must not throw any exception. Otherwise application will be terminated.
     *
     * @return true if service is available else false
     */
    boolean ping();
}
