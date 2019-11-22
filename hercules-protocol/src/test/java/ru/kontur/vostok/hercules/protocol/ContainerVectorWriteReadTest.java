package ru.kontur.vostok.hercules.protocol;

import org.junit.Test;
import ru.kontur.vostok.hercules.protocol.decoder.ContainerVectorReader;
import ru.kontur.vostok.hercules.protocol.encoder.ContainerVectorWriter;

import java.util.Collections;

public class ContainerVectorWriteReadTest {
    @Test
    public void shouldWriteReadContainerArray() {
        Container[] containers = new Container[]{
                Container.of("fist", Variant.ofString("first")),
                Container.of("second", Variant.ofString("second"))
        };

        WriteReadPipe<Container[]> pipe = WriteReadPipe.init(new ContainerVectorWriter(), new ContainerVectorReader());

        pipe.process(containers).assertEquals(
                (containers1, containers2) -> HerculesProtocolAssert.assertArrayEquals(
                        containers1,
                        containers2,
                        HerculesProtocolAssert::assertEquals)
        );
    }
}
