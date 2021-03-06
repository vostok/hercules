package ru.kontur.vostok.hercules.protocol.encoder;

import ru.kontur.vostok.hercules.protocol.Container;

/**
 * Hercules Protocol Writer for array of container
 *
 * @author Daniil Zhenikhov
 */
public class ContainerArrayWriter implements Writer<Container[]> {
    public static final ContainerArrayWriter INSTANCE = new ContainerArrayWriter();

    private static final ContainerWriter CONTAINER_WRITER = ContainerWriter.INSTANCE;

    /**
     * Write containers' array with encoder
     *
     * @param encoder Encoder for write data and pack with specific format
     * @param value   Array of containers which are must be written
     */
    @Override
    public void write(Encoder encoder, Container[] value) {
        encoder.writeInteger(value.length);

        for (Container container : value) {
            CONTAINER_WRITER.write(encoder, container);
        }
    }
}
