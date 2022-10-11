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
    private final Reader<Variant> variantReader;

    private final Set<TinyString> tags;

    private ContainerReader(Set<TinyString> tags, Reader<Variant> variantReader) {
        this.tags = tags;
        this.variantReader = variantReader;
    }

    public static ContainerReader readAllTags() {
        return readAllTags(new VariantReader());
    }

    public static ContainerReader readAllTags(Reader<Variant> variantReader) {
        return new ContainerReader(null, variantReader);
    }

    public static ContainerReader readTags(Set<TinyString> tags) {
        return readTags(tags, new VariantReader());
    }

    public static ContainerReader readTags(Set<TinyString> tags, Reader<Variant> variantReader) {
        return new ContainerReader(tags, variantReader);
    }


    @Override
    public Container read(Decoder decoder) {
        int length = decoder.readContainerSize();
        Map<TinyString, Variant> variantMap = new HashMap<>(Maps.effectiveHashMapCapacity(length));
        while (0 <= --length) {
            TinyString tagName = decoder.readTinyString();
            if (Objects.isNull(tags) || tags.contains(tagName)) {
                Variant variant = variantReader.read(decoder);
                variantMap.put(tagName, variant);
            } else {
                variantReader.skip(decoder);
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
            variantReader.skip(decoder);
        }
        return decoder.position() - position;
    }
}
