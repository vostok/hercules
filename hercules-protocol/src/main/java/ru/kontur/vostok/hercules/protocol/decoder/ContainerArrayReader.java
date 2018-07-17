package ru.kontur.vostok.hercules.protocol.decoder;

import ru.kontur.vostok.hercules.protocol.Container;

public class ContainerArrayReader implements Reader<Container[]> {
    public static final ContainerArrayReader INSTANCE = new ContainerArrayReader();

    private static final ContainerReader containerReader = ContainerReader.INSTANCE;

    @Override
    public Container[] read(Decoder decoder) {
        int length = decoder.readArrayLength();
        Container[] containers = new Container[length];

        for (int index = 0; index < length; index++) {
            containers[index] = containerReader.read(decoder);
        }

        return containers;
    }

    @Override
    public int skip(Decoder decoder) {
        int length = decoder.readArrayLength();
        int skipped = 0;

        while (0 <= --length) {
            skipped += containerReader.skip(decoder);
        }

        return skipped + SizeOf.ARRAY_LENGTH;
    }
}
