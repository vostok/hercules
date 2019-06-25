package ru.kontur.vostok.hercules.kafka.util.processing.single;

import ru.kontur.vostok.hercules.kafka.util.processing.BackendServiceFailedException;

/**
 * SingleSender
 *
 * @author Kirill Sulim
 */
@Deprecated
public interface SingleSender<Key, Value> extends AutoCloseable {

    boolean process(Key key, Value value) throws BackendServiceFailedException;
}
