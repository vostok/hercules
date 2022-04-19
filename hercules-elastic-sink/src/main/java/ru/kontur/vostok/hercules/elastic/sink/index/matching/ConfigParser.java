package ru.kontur.vostok.hercules.elastic.sink.index.matching;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import ru.kontur.vostok.hercules.protocol.hpath.HPath;
import ru.kontur.vostok.hercules.util.Maps;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Configures MatchingIndexResolver.
 * Parses raw index data array from JSON and transform its contents to usable object types
 *
 * @author Petr Demenev
 */
public class ConfigParser {

    public static IndexData[] parse(InputStream in) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectReader reader = mapper.readerFor(new TypeReference<IndexDataModel[]>() {});
        IndexDataModel[] indices;
        try {
            indices = reader.readValue(in);
        } catch (IOException ex) {
            throw new IllegalArgumentException("Cannot parse index map", ex);
        }
        return Arrays.stream(indices).
                map(i -> new IndexData(i.getIndex(), transform(i.getTagMaps()))).
                toArray(IndexData[]::new);
    }

    private static List<TagMap> transform(List<Map<String, String>> list) {
        return list.stream().
                map(sourceMap -> {
                    Map<HPath, Pattern> resultMap = new HashMap<>(Maps.effectiveHashMapCapacity(sourceMap.size()));
                    sourceMap.forEach((key, value) -> resultMap.put(
                            HPath.fromPath(key),
                            Pattern.compile(value == null ? "null" : value)));
                    return new TagMap(resultMap);
                }).
                collect(Collectors.toList());
    }
}
