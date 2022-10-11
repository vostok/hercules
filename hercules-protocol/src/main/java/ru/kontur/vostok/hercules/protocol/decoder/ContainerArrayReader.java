package ru.kontur.vostok.hercules.protocol.decoder;

import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Type;

/**
 * Hercules Protocol Reader for reading array of containers
 *
 * @author Daniil Zhenikhov
 */
public class ContainerArrayReader implements Reader<Container[]> {
    public static final ContainerArrayReader INSTANCE = new ContainerArrayReader();

    private static final ContainerReader CONTAINER_READER = ContainerReader.readAllTags();

    /**
     * Read array of container  with decoder
     *
     * @param decoder Decoder for read data and unpack with specific format
     * @return array of containers
     */
    @Override
    public Container[] read(Decoder decoder) {
        int length = decoder.readInteger();
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
        int length = decoder.readInteger();
        int skipped = 0;

        while (0 <= --length) {
            skipped += CONTAINER_READER.skip(decoder);
        }

        return skipped + Type.INTEGER.size;
    }
}
