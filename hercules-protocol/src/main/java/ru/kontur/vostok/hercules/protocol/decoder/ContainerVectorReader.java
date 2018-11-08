package ru.kontur.vostok.hercules.protocol.decoder;

import ru.kontur.vostok.hercules.protocol.Container;

/**
 * Hercules Protocol Reader for reading vector of containers
 *
 * @author Daniil Zhenikhov
 */
public class ContainerVectorReader implements Reader<Container[]> {
    public static final ContainerVectorReader INSTANCE = new ContainerVectorReader();

    private static final ContainerReader CONTAINER_READER = ContainerReader.INSTANCE;

    /**
     * Read vector of container  with decoder
     *
     * @param decoder Decoder for read data and unpack with specific format
     * @return vector of containers
     */
    @Override
    public Container[] read(Decoder decoder) {
        int length = decoder.readVectorLength();
        Container[] containers = new Container[length];

        for (int index = 0; index < length; index++) {
            containers[index] = CONTAINER_READER.read(decoder);
        }

        return containers;
    }

    /**
     * Skip array of container with decoder
     *
     * @param decoder Decoder for read data and unpack with specific format
     * @return count of byte which must be skipped
     */
    @Override
    public int skip(Decoder decoder) {
        int length = decoder.readVectorLength();
        int skipped = 0;

        while (0 <= --length) {
            skipped += CONTAINER_READER.skip(decoder);
        }

        return skipped + SizeOf.VECTOR_LENGTH;
    }
}
