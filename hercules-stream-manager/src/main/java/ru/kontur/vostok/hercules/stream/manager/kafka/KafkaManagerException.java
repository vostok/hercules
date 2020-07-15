package ru.kontur.vostok.hercules.stream.manager.kafka;

/**
 * Exception can be thrown by {@link KafkaManager} methods.
 *
 * @author Gregory Koshelev
 */
public class KafkaManagerException extends Exception {
    KafkaManagerException(Throwable throwable) {
        super(throwable);
    }
}
