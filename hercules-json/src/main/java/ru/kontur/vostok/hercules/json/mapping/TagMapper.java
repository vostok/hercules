package ru.kontur.vostok.hercules.json.mapping;

import ru.kontur.vostok.hercules.json.DocumentUtil;
import ru.kontur.vostok.hercules.json.transformer.Transformer;
import ru.kontur.vostok.hercules.protocol.TinyString;
import ru.kontur.vostok.hercules.protocol.Variant;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Maps tag using the destination path.
 *
 * @author Gregory Koshelev
 */
public class TagMapper implements Mapper {
    private final List<String> path;
    private final String field;
    private final Transformer transformer;

    public TagMapper(String destination, Transformer transformer) {
        String[] segments = destination.split("/");

        this.path = segments.length > 1
                ? Arrays.asList(Arrays.copyOfRange(segments, 0, segments.length - 1))
                : Collections.emptyList();
        this.field = segments[segments.length - 1];
        this.transformer = transformer;
    }

    public void map(TinyString tag, Variant value, Map<String, Object> document) {
        DocumentUtil.subdocument(document, path).putIfAbsent(field, transformer.transform(value));
    }
}
