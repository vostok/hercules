package ru.kontur.vostok.hercules.protocol;

import org.junit.Test;
import ru.kontur.vostok.hercules.protocol.decoder.ContainerArrayReader;
import ru.kontur.vostok.hercules.protocol.encoder.ContainerArrayWriter;

import java.util.Collections;

public class ContainerArrayWriteReadTest {
    @Test
    public void shouldWriteReadContainerArray() throws Exception {
        Container[] containers = TestUtil.multiply(new Container[]{
                new Container(Collections.singletonMap("fist", Variant.ofString("first"))),
                new Container(Collections.singletonMap("second", Variant.ofString("second")))
        }, 100);

        WriteReadPipe<Container[]> pipe = WriteReadPipe.init(new ContainerArrayWriter(), new ContainerArrayReader());

        pipe.process(containers).assertEquals(
                (containers1, containers2) -> HerculesProtocolAssert.assertArrayEquals(
                        containers1,
                        containers2,
                        HerculesProtocolAssert::assertEquals)
        );
    }
}
