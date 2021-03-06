package ru.kontur.vostok.hercules.json.format;

import ru.kontur.vostok.hercules.json.Document;
import ru.kontur.vostok.hercules.json.format.transformer.Transformer;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.protocol.hpath.HPath;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Maps the tag value to the document's field using {@link Transformer}.
 *
 * @author Gregory Koshelev
 */
public class TransformMapper implements Mapper {
    private final HPath sourcePath;
    private final List<String> destinationPath;
    private final String field;
    private final Transformer transformer;

    public TransformMapper(HPath sourcePath, String destination, Transformer transformer) {
        this.sourcePath = sourcePath;

        String[] segments = destination.split("/");
        this.destinationPath = segments.length > 1
                ? Arrays.asList(Arrays.copyOfRange(segments, 0, segments.length - 1))
                : Collections.emptyList();
        this.field = segments[segments.length - 1];

        this.transformer = transformer;
    }

    public void map(Event event, Document document) {
        Variant value = sourcePath.extract(event.getPayload());
        if (value == null) {
            return;
        }
        Object result = transformer.transform(value);
        if (result == null) {
            return;
        }
        document.subdocument(destinationPath).putIfAbsent(field, result);
    }
}
