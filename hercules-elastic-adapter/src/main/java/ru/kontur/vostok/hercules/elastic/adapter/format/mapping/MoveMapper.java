package ru.kontur.vostok.hercules.elastic.adapter.format.mapping;

import ru.kontur.vostok.hercules.elastic.adapter.format.ProtoContainer;
import ru.kontur.vostok.hercules.json.Document;
import ru.kontur.vostok.hercules.protocol.TinyString;
import ru.kontur.vostok.hercules.protocol.hpath.HPath;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Moves a whole sub-document preserving field names as tags.
 * <p>
 * Some source fields may be ignored during the movement.
 *
 * @author Gregory Koshelev
 */
public class MoveMapper implements Mapper {
    private final List<String> sourcePath;
    private final HPath destinationPath;
    private final Set<String> exceptedFields;

    public MoveMapper(String source, HPath destinationPath, Set<String> exceptedFields) {
        this.destinationPath = destinationPath;
        this.sourcePath = source.isEmpty() ? Collections.emptyList() : Arrays.asList(source.split("/"));
        this.exceptedFields = exceptedFields;
    }

    @Override
    public void map(Document src, ProtoContainer dest) {
        Object fields = src.get(sourcePath);
        if (!(fields instanceof Map)) {
            return;
        }

        for (Map.Entry<String, Object> field : ((Map<String, Object>) fields).entrySet()) {
            String key = field.getKey();
            if (exceptedFields.contains(key)) {
                continue;
            }
            dest.put(
                    HPath.combine(destinationPath, TinyString.of(key)),
                    Transformer.PLAIN.transform(field.getValue()));
        }
    }
}
