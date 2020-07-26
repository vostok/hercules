package ru.kontur.vostok.hercules.json.format;

import ru.kontur.vostok.hercules.json.DocumentUtil;
import ru.kontur.vostok.hercules.json.format.transformer.Transformer;
import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.TinyString;
import ru.kontur.vostok.hercules.protocol.Type;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.protocol.hpath.HPath;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Maps the whole container tag preserving internal structure using {@link Transformer#PLAIN}.
 *
 * @author Gregory Koshelev
 */
public class MoveMapper implements Mapper {
    private final HPath sourcePath;
    private final List<String> destinationPath;
    private final Set<TinyString> exceptedTags;

    public MoveMapper(HPath sourcePath, String destination, Set<TinyString> exceptedTags) {
        this.sourcePath = sourcePath;
        this.destinationPath = destination.isEmpty() ? Collections.emptyList() : Arrays.asList(destination.split("/"));
        this.exceptedTags = exceptedTags;
    }

    @Override
    public void map(Event event, Map<String, Object> document) {
        Variant value = sourcePath.extract(event.getPayload());
        if (value == null) {
            return;
        }
        if (value.getType() != Type.CONTAINER) {
            throw new IllegalArgumentException("Expect container but got " + value.getType());
        }
        writeContainer(DocumentUtil.subdocument(document, destinationPath), (Container) value.getValue());
    }

    private void writeContainer(Map<String, Object> document, Container container) {
        for (Map.Entry<TinyString, Variant> tag : container.tags().entrySet()) {
            if (!exceptedTags.contains(tag.getKey())) {
                document.putIfAbsent(tag.getKey().toString(), Transformer.PLAIN.transform(tag.getValue()));
            }
        }
    }
}
