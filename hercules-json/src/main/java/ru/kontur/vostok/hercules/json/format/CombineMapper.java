package ru.kontur.vostok.hercules.json.format;

import ru.kontur.vostok.hercules.json.DocumentUtil;
import ru.kontur.vostok.hercules.json.format.combiner.Combiner;
import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.protocol.hpath.HPath;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Combines two or more tag values to the single field using {@link Combiner}.
 *
 * @author Gregory Koshelev
 */
public class CombineMapper implements Mapper {
    private final List<HPath> sourcePaths;
    private final List<String> destinationPath;
    private final String field;
    private final Combiner combiner;

    public CombineMapper(List<HPath> sourcePaths, String destination, Combiner combiner) {
        this.sourcePaths = sourcePaths;

        String[] segments = destination.split("/");
        this.destinationPath = segments.length > 1
                ? Arrays.asList(Arrays.copyOfRange(segments, 0, segments.length - 1))
                : Collections.emptyList();
        this.field = segments[segments.length - 1];

        this.combiner = combiner;
    }

    @Override
    public void map(Event event, Map<String, Object> document) {
        Container payload = event.getPayload();

        Variant[] values = new Variant[sourcePaths.size()];
        int i = 0;
        for (HPath sourcePath : sourcePaths) {
            Variant value = sourcePath.extract(payload);
            if (value == null) {
                return;
            }
            values[i++] = value;
        }

        DocumentUtil.subdocument(document, destinationPath).putIfAbsent(field, combiner.combine(values));
    }
}
