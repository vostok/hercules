package ru.kontur.vostok.hercules.protocol.decoder;

import ru.kontur.vostok.hercules.protocol.Container;

/**
 * Hercules Protocol Reader for reading vector of containers
 *
 * @author Daniil Zhenikhov
 */
public class ContainerVectorReader implements Reader<Container[]> {
    public static final ContainerVectorReader INSTANCE = new ContainerVectorReader();

    private static final ContainerReader CONTAINER_READER = ContainerReader.readAllTags();

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
        int position = decoder.position();

        int length = decoder.readVectorLength();
        while (0 <= --length) {
            CONTAINER_READER.skip(decoder);
        }

        return decoder.position() - position;
    }
}
