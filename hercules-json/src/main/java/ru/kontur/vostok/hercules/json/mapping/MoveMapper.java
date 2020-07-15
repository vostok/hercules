package ru.kontur.vostok.hercules.json.mapping;

import ru.kontur.vostok.hercules.json.DocumentUtil;
import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.TinyString;
import ru.kontur.vostok.hercules.protocol.Type;
import ru.kontur.vostok.hercules.protocol.Variant;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Maps whole container tag preserving internal structure using {@link Mapper#PLAIN}.
 *
 * @author Gregory Koshelev
 */
public class MoveMapper implements Mapper {
    private final List<String> path;

    public MoveMapper(String destination) {
        this.path = destination.isEmpty() ? Collections.emptyList() : Arrays.asList(destination.split("/"));
    }

    @Override
    public void map(TinyString tag, Variant value, Map<String, Object> document) {
        if (value.getType() != Type.CONTAINER) {
            throw new IllegalArgumentException("Expect container but got " + value.getType());
        }
        writeContainer(DocumentUtil.subdocument(document, path), (Container) value.getValue());
    }

    private void writeContainer(Map<String, Object> document, Container container) {
        for (Map.Entry<TinyString, Variant> tag : container.tags().entrySet()) {
            PLAIN.map(tag.getKey(), tag.getValue(), document);
        }
    }
}
