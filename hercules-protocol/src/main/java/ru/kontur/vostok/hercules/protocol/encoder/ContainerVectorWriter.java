package ru.kontur.vostok.hercules.protocol.encoder;

import ru.kontur.vostok.hercules.protocol.Container;

public class ContainerVectorWriter implements Writer<Container[]> {
    public static final ContainerVectorWriter INSTANCE = new ContainerVectorWriter();

    private static final ContainerWriter containerWriter = ContainerWriter.INSTANCE;

    @Override
    public void write(Encoder encoder, Container[] value) {
        encoder.writeVectorLength(value.length);

        for (Container container : value) {
            containerWriter.write(encoder, container);
        }
    }
}
