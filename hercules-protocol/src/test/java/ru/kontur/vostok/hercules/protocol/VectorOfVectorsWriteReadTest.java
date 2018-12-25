package ru.kontur.vostok.hercules.protocol;

import org.junit.Test;
import ru.kontur.vostok.hercules.protocol.decoder.ContainerReader;
import ru.kontur.vostok.hercules.protocol.encoder.ContainerWriter;

import java.util.Collections;

/**
 * @author Gregory Koshelev
 */
public class VectorOfVectorsWriteReadTest {
    @Test
    public void shouldWriteReadContainerWithVectorOfVectors() {
        Container container = new Container(
                Collections.singletonMap(
                        "vector",
                        Variant.ofVector(Vector.ofVectors(Vector.ofIntegers(1, 2), Vector.ofIntegers(3, 4)))));

        WriteReadPipe<Container> pipe = WriteReadPipe.init(new ContainerWriter(), ContainerReader.readAllTags());

        pipe.process(container).assertEquals(HerculesProtocolAssert::assertEquals);
    }
}
