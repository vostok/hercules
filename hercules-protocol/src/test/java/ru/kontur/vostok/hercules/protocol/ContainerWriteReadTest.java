package ru.kontur.vostok.hercules.protocol;

import org.junit.Test;
import ru.kontur.vostok.hercules.protocol.decoder.ContainerReader;
import ru.kontur.vostok.hercules.protocol.encoder.ContainerWriter;

import java.util.HashMap;
import java.util.Map;

public class ContainerWriteReadTest {

    private final WriteReadPipe<Container> pipe = WriteReadPipe.init(new ContainerWriter(), new ContainerReader());



    @Test
    public void shouldReadWriteContainer() throws Exception {
        Map<String, Variant> variantMap = new HashMap<>();
        variantMap.put("int-sample", Variant.ofInteger(123));
        variantMap.put("text-sample", Variant.ofText("Abc еёю"));

        Container container = new Container(variantMap);
        pipe.process(container)
                .assertEquals((exp, act) -> {
                    for (Map.Entry<String, Variant> entry : exp) {
                        HerculesProtocolAssert.assertEquals(entry.getValue(), act.get(entry.getKey()));
                    }
                });
    }

    @Test
    public void shouldReadWrite255Variants() throws Exception {
        Map<String, Variant> variantMap = new HashMap<>();

        for (int i = 0; i < VectorConstants.VECTOR_LENGTH_EXCEEDED - 1; ++i) {
            variantMap.put(String.valueOf(i), Variant.ofInteger(i));
        }


        Container container = new Container(variantMap);
        pipe.process(container)
                .assertEquals((exp, act) -> {
                    for (Map.Entry<String, Variant> entry : exp) {
                        HerculesProtocolAssert.assertEquals(entry.getValue(), act.get(entry.getKey()));
                    }
                });
    }
}
