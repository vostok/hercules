package ru.kontur.vostok.hercules.protocol;

import org.junit.Test;
import ru.kontur.vostok.hercules.protocol.util.ContainerBuilder;
import ru.kontur.vostok.hercules.protocol.util.ContainerUtil;
import ru.kontur.vostok.hercules.protocol.util.TagDescription;
import ru.kontur.vostok.hercules.protocol.util.TagDescriptionBuilder;

/**
 * @author Gregory Koshelev
 */
public class VectorWriteReadTest {
    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowException() throws Exception {
        Container container = ContainerBuilder.create()
                .tag("abc", Variant.ofVector(Vector.ofStrings(new String[]{"def"})))
                .build();

        TagDescription<String> tag = TagDescriptionBuilder.string("abc").build();

        String extracted = ContainerUtil.extract(container, tag);
    }

}
