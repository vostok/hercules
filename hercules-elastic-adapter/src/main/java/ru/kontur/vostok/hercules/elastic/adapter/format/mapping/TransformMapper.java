package ru.kontur.vostok.hercules.elastic.adapter.format.mapping;

import ru.kontur.vostok.hercules.elastic.adapter.format.ProtoContainer;
import ru.kontur.vostok.hercules.json.Document;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.protocol.hpath.HPath;

import java.util.Arrays;
import java.util.List;

/**
 * Maps the field to the tag.
 *
 * @author Gregory Koshelev
 */
public class TransformMapper implements Mapper {
    private final List<String> sourcePath;
    private final HPath destinationPath;
    private final Transformer transformer;

    public TransformMapper(String source, HPath destinationPath, Transformer transformer) {
        this.sourcePath = Arrays.asList(source.split("/"));
        this.destinationPath = destinationPath;
        this.transformer = transformer;
    }

    @Override
    public void map(Document src, ProtoContainer dest) {
        Object value = src.get(sourcePath);
        if (value == null) {
            return;
        }
        Variant result = transformer.transform(value);
        if (result == null) {
            return;
        }
        dest.put(destinationPath, result);
    }
}
