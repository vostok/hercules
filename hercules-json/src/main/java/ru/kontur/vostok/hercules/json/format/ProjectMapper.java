package ru.kontur.vostok.hercules.json.format;

import ru.kontur.vostok.hercules.json.Document;
import ru.kontur.vostok.hercules.json.format.transformer.Transformer;
import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.TinyString;
import ru.kontur.vostok.hercules.protocol.Type;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.protocol.Vector;
import ru.kontur.vostok.hercules.protocol.hpath.HPath;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Maps the {@link Container container} or the {@link Vector vector} of containers to the document using
 * the value of the key tag as the field name and the value of the value tag as the field value.
 *
 * @author Gregory Koshelev
 */
public class ProjectMapper implements Mapper {
    private final HPath sourcePath;
    private final List<String> destinationPath;
    private final TinyString keyTag;
    private final Transformer keyTransformer;
    private final TinyString valueTag;
    private final Transformer valueTransformer;

    public ProjectMapper(
            HPath sourcePath,
            String destination,
            TinyString keyTag,
            Transformer keyTransformer,
            TinyString valueTag,
            Transformer valueTransformer) {
        this.sourcePath = sourcePath;
        this.destinationPath = destination.isEmpty() ? Collections.emptyList() : Arrays.asList(destination.split("/"));
        this.keyTag = keyTag;
        this.keyTransformer = keyTransformer;
        this.valueTag = valueTag;
        this.valueTransformer = valueTransformer;
    }

    @Override
    public void map(Event event, Document document) {
        Variant value = sourcePath.extract(event.getPayload());
        if (value == null) {
            return;
        }

        Map<String, Object> subdocument = document.subdocument(destinationPath);

        if (value.getType() == Type.CONTAINER) {
            writeKV(subdocument, (Container) value.getValue());
        } else if (value.getType() == Type.VECTOR) {
            Vector vector = (Vector) value.getValue();
            if (vector.getType() != Type.CONTAINER) {
                return;
            }

            Container[] containers = (Container[]) vector.getValue();
            for (Container container : containers) {
                writeKV(subdocument, container);
            }
        }
    }

    private void writeKV(Map<String, Object> document, Container container) {
        Variant keyVar = container.get(keyTag);
        Variant valueVar = container.get(valueTag);
        if (keyVar == null || valueVar == null) {
            return;
        }

        Object key = keyTransformer.transform(keyVar);
        Object value = valueTransformer.transform(valueVar);
        if (key == null || value == null) {
            return;
        }

        document.putIfAbsent(key.toString(), value);
    }
}
