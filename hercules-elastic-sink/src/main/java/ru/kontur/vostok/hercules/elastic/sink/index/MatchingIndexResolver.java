package ru.kontur.vostok.hercules.elastic.sink.index;

import ru.kontur.vostok.hercules.configuration.Sources;
import ru.kontur.vostok.hercules.elastic.sink.index.matching.ConfigParser;
import ru.kontur.vostok.hercules.elastic.sink.index.matching.IndexData;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.util.parameter.Parameter;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;

import java.util.Optional;
import java.util.Properties;

/**
 * Resolves the index name by choice from file with predefined indices and corresponding tag values.
 * <p>
 * File format rules:
 * <ul>
 *   <li> File content must be in JSON format.
 *   <li> The content consist of index data array.
 *   <li> Value of the field {@code index} is an index name.
 *   <li> Value of the field {@code tagMaps} is an array of tag maps.
 *   <li> Every tag map consist of set of tag-value pairs.
 *   <li> Tag value may be present as plain string or regular expressions.
 * </ul>
 * <p>
 * File content sample:
 * <pre>{@code
 * [
 *   {
 *     "index": "first-index",
 *     "tagMaps": [
 *       {
 *         "some-tag": "some-value",
 *         "outer-tag/inner-tag": "inner-value"
 *       },
 *       {
 *         "some-tag": "alternative-value"
 *       }
 *     ]
 *   },
 *   {
 *     "index": "second-index",
 *     "tagMaps": [
 *       {
 *         "some-tag": "for-second-index-value"
 *       },
 *       {
 *         "special-tag": "special-value"
 *       }
 *     ]
 *   }
 * ]}</pre>
 * The event matches the index if the event contains all tags from at least one of index tag maps. <p>
 * If the event matches one or more indices then the name of first of these indices is returned with {@link Optional} wrap. <p>
 * If the event doesn't match all indices then the resolver returns empty {@link Optional}. <p>
 * <br>
 * Example for above file content sample:
 * <li> for event with single tag {@code 'some-tag=for-second-index-value'} index {@code 'second-index'} will be resolved,
 * <li> for event with single tag {@code 'special-tag=special-value'} index {@code 'second-index'} will be resolved,
 * <li> for event with tags {@code 'some-tag=some-value'} and {@code 'outer-tag/inner-tag=inner-value'}
 * without other tags, index {@code 'first-index'} will be resolved,
 * <li> for event with tags {@code 'some-tag=alternative-value'} and {@code 'special-tag=special-value'}
 * without other tags, index {@code 'first-index'} will be resolved,
 * <li> for event with tags {@code 'some-tag=some-value'} and {@code 'special-tag=non-special-value'}
 * without other tags, no index will be resolved.
 *
 * @author Petr Demenev
 */
public class MatchingIndexResolver extends IndexResolver {

    private final IndexData[] indices;

    public MatchingIndexResolver(Properties properties) {
        super(properties);
        String filePath = PropertiesUtil.get(Props.FILE_PATH, properties).get();
        indices = ConfigParser.parse(Sources.load(filePath));
    }

    @Override
    public Optional<String> resolve(Event event) {
        for (IndexData index : indices) {
            boolean matches = index.getTagMaps().stream().anyMatch(tagMap -> tagMap.matches(event));
            if (matches) {
                return Optional.of(index.getIndex());
            }
        }
        return Optional.empty();
    }

    private static class Props {
        private static final Parameter<String> FILE_PATH = Parameter.stringParameter("file").
                withDefault("file://indices.json").
                build();
    }
}
