package ru.kontur.vostok.hercules.protocol.encoder;

import ru.kontur.vostok.hercules.protocol.Container;

/**
 * Hercules Protocol Writer for vector of containers
 * @author jdk
 */
public class ContainerVectorWriter implements Writer<Container[]> {
    public static final ContainerVectorWriter INSTANCE = new ContainerVectorWriter();

    private static final ContainerWriter containerWriter = ContainerWriter.INSTANCE;

    /**
     * Write containers' array with encoder.
     * @param encoder Encoder for write data
     * @param value Array of containers which are must be written
     */
    @Override
    public void write(Encoder encoder, Container[] value) {
        encoder.writeVectorLength(value.length);

        for (Container container : value) {
            containerWriter.write(encoder, container);
        }
    }
}
