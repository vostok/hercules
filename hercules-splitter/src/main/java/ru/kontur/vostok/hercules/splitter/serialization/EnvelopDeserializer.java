package ru.kontur.vostok.hercules.splitter.serialization;

import org.apache.kafka.common.serialization.Deserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.protocol.decoder.Decoder;
import ru.kontur.vostok.hercules.protocol.decoder.Reader;
import ru.kontur.vostok.hercules.splitter.models.Envelope;

import java.nio.ByteBuffer;
import java.util.function.Supplier;

/**
 * Deserializer implementation of {@link Envelope} for Kafka client library.
 *
 * @author Aleksandr Yuferov
 */
public class EnvelopDeserializer implements Deserializer<Envelope> {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvelopDeserializer.class);

    private final Supplier<Reader<String>> destinationReaderFactory;

    /**
     * Constructor.
     *
     * @param destinationReaderFactory Destination reader factory is used to get the reader for {@link Envelope#destination()} property.
     */
    public EnvelopDeserializer(Supplier<Reader<String>> destinationReaderFactory) {
        this.destinationReaderFactory = destinationReaderFactory;
    }

    @Override
    public Envelope deserialize(String topic, byte[] data) {
        if (data == null) {
            return null;
        }
        try {
            Reader<String> destinationReader = destinationReaderFactory.get();
            String destination = destinationReader.read(new Decoder(ByteBuffer.wrap(data)));
            return new Envelope(data, destination);
        } catch (Exception exception) {
            LOGGER.error("an error occurred while deserializing the envelope", exception);
            return null;
        }
    }
}
