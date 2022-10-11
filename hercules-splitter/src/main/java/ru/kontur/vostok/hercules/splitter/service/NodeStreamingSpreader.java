package ru.kontur.vostok.hercules.splitter.service;

import javax.annotation.concurrent.NotThreadSafe;

/**
 * Interface of spreaders of the data between the nodes.
 *
 * @author Aleksandr Yuferov
 */
@NotThreadSafe
public interface NodeStreamingSpreader extends StreamingAlgorithm {

    /**
     * Calculates the name of the destination node.
     *
     * @return Destination node name.
     */
    String destination();
}
