package ru.kontur.vostok.hercules.splitter.serialization;

import org.apache.kafka.common.serialization.Serializer;
import ru.kontur.vostok.hercules.splitter.models.Envelope;

/**
 * Serializer implementation of {@link Envelope} for Kafka client library.
 * <p>
 * There is no need in serialization because the {@link Envelope} contains serialized event data. Implementation of this serializer simply returns this data
 * from given {@link Envelope} object.
 *
 * @author Aleksandr Yuferov
 */
public class EnvelopeSerializer implements Serializer<Envelope> {

    @Override
    public byte[] serialize(String topic, Envelope envelope) {
        return envelope.data();
    }
}
