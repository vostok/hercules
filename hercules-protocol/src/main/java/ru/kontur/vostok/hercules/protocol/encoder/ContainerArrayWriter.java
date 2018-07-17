package ru.kontur.vostok.hercules.protocol.encoder;

import ru.kontur.vostok.hercules.protocol.Container;

public class ContainerArrayWriter implements Writer<Container[]> {
    public static final ContainerArrayWriter INSTANCE = new ContainerArrayWriter();

    private static final ContainerWriter containerWriter = ContainerWriter.INSTANCE;

    @Override
    public void write(Encoder encoder, Container[] value) {
        encoder.writeArrayLength(value.length);

        for (Container container : value) {
            containerWriter.write(encoder, container);
        }
    }
}
