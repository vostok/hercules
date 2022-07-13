package ru.kontur.vostok.hercules.routing.engine;

import ru.kontur.vostok.hercules.routing.Destination;
import ru.kontur.vostok.hercules.routing.config.ConfigurationObserver;

/**
 * Interface of router engines.
 * <p>
 * Implementations of this interface performs routing using given configuration.
 *
 * @author Aleksandr Yuferov
 */
public interface RouterEngine<Q> extends ConfigurationObserver {
    /**
     * Perform routing of given object.
     *
     * @param query Query object with the help of engine can make decision about destination.
     * @return Destination of something related to a query.
     */
    Destination<?> route(Q query);
}
