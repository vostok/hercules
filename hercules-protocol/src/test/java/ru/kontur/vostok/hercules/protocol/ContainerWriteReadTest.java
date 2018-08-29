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
    public void shouldReadWriteContainer() throws Exception {
        Map<String, Variant> variantMap = new HashMap<>();
        variantMap.put("int-sample", Variant.ofInteger(123));
        variantMap.put("text-sample", Variant.ofText("Abc еёю"));

        Container container = new Container(variantMap);
        pipe.process(container).assertEquals(HerculesProtocolAssert::assertEquals);
    }

    @Test
    public void shouldReadWriteMaxCountOfVariants() throws Exception {
        Map<String, Variant> variantMap = new HashMap<>();

        for (int i = 0; i < Short.MAX_VALUE; ++i) {
            variantMap.put(String.valueOf(i), Variant.ofInteger(i));
        }


        Container container = new Container(variantMap);
        pipe.process(container).assertEquals(HerculesProtocolAssert::assertEquals);
    }

    @Test
    public void shouldSkipUnmarkedTags() throws Exception {
        Map<String, Variant> full = new HashMap<>();
        full.put("keep", Variant.ofInteger(1));
        full.put("skip", Variant.ofInteger(2));
        Container fullContainer = new Container(full);

        Map<String, Variant> filtered = new HashMap<>();
        filtered.put("keep", Variant.ofInteger(1));
        Container filteredContainer = new Container(filtered);

        WriteReadPipe<Container> filterPipe = WriteReadPipe.init(new ContainerWriter(), ContainerReader.readTags(Collections.singleton("keep")));
        Container processed = filterPipe.process(fullContainer).getProcessed();

        HerculesProtocolAssert.assertEquals(filteredContainer, processed);
    }

    @Test
    public void shouldReadWriteInnerContainer() throws Exception {
        Map<String, Variant> innerVariantMap = new HashMap<>();
        innerVariantMap.put("first", Variant.ofInteger(1));
        innerVariantMap.put("second", Variant.ofStringArray(new String[]{"a", "b", "c"}));
        Container innerContainer = new Container(innerVariantMap);

        Map<String, Variant> variantMap = Collections.singletonMap("inner", Variant.ofContainer(innerContainer));
        Container container = new Container(variantMap);

        pipe.process(container).assertEquals(HerculesProtocolAssert::assertEquals);
    }
}
