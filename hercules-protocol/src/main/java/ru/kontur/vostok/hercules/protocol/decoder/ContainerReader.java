package ru.kontur.vostok.hercules.protocol.decoder;

import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.TinyString;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.util.Maps;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class ContainerReader implements Reader<Container> {

    public static final ContainerReader INSTANCE = readAllTags();

    private static final VariantReader VARIANT_READER = VariantReader.INSTANCE;

    private final Set<TinyString> tags;

    private ContainerReader(Set<TinyString> tags) {
        this.tags = tags;
    }

    public static ContainerReader readAllTags() {
        return new ContainerReader(null);
    }

    public static ContainerReader readTags(Set<TinyString> tags) {
        return new ContainerReader(tags);
    }

    @Override
    public Container read(Decoder decoder) {
        int length = decoder.readContainerSize();
        Map<TinyString, Variant> variantMap = new HashMap<>(Maps.effectiveHashMapCapacity(length));
        while (0 <= --length) {
            TinyString tagName = decoder.readTinyString();
            if (Objects.isNull(tags) || tags.contains(tagName)) {
                Variant variant = VARIANT_READER.read(decoder);
                variantMap.put(tagName, variant);
            } else {
                VARIANT_READER.skip(decoder);
            }
        }
        return Container.of(variantMap);
    }

    @Override
    public int skip(Decoder decoder) {
        int position = decoder.position();

        int length = decoder.readContainerSize();
        while (0 <= --length) {
            decoder.skipTinyString();
            VARIANT_READER.skip(decoder);
        }
        return decoder.position() - position;
    }
}
