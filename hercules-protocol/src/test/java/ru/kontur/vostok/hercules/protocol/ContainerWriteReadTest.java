package ru.kontur.vostok.hercules.protocol;

import org.junit.Test;
import ru.kontur.vostok.hercules.protocol.decoder.ContainerReader;
import ru.kontur.vostok.hercules.protocol.encoder.ContainerWriter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ContainerWriteReadTest {

    private final WriteReadPipe<Container> pipe = WriteReadPipe.init(new ContainerWriter(), ContainerReader.readAllTags());

    @Test
    public void shouldReadWriteContainer() {
        Container container = Container.builder().
                tag("int-sample", Variant.ofInteger(123)).
                tag("text-sample", Variant.ofString("Abc еёю")).
                build();

        pipe.process(container).assertEquals(HerculesProtocolAssert::assertEquals);
    }

    @Test
    public void shouldReadWriteMaxCountOfVariants() {
        Map<TinyString, Variant> variantMap = new HashMap<>();

        for (int i = 0; i < Short.MAX_VALUE; ++i) {
            variantMap.put(TinyString.of(String.valueOf(i)), Variant.ofInteger(i));
        }

        Container container = Container.of(variantMap);
        pipe.process(container).assertEquals(HerculesProtocolAssert::assertEquals);
    }

    @Test
    public void shouldSkipUnmarkedTags() {
        Container fullContainer = Container.builder().
                tag("keep", Variant.ofInteger(1)).
                tag("skip", Variant.ofInteger(2)).
                build();

        Container filteredContainer = Container.of("keep", Variant.ofInteger(1));

        WriteReadPipe<Container> filterPipe = WriteReadPipe.init(new ContainerWriter(), ContainerReader.readTags(Collections.singleton(TinyString.of("keep"))));
        Container processed = filterPipe.process(fullContainer).getProcessed();

        HerculesProtocolAssert.assertEquals(filteredContainer, processed);
    }

    @Test
    public void shouldReadWriteInnerContainer() {
        Container innerContainer = Container.builder().
                tag("first", Variant.ofInteger(1)).
                tag("second", Variant.ofVector(Vector.ofStrings("a", "b", "c"))).
                build();

        Container container = Container.of("inner", Variant.ofContainer(innerContainer));

        pipe.process(container).assertEquals(HerculesProtocolAssert::assertEquals);
    }
}
