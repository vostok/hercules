package ru.kontur.vostok.hercules.elastic.sink.index;

import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.Type;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.protocol.hpath.HPath;
import ru.kontur.vostok.hercules.util.parameter.Parameter;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

/**
 * Resolves the index name from the event data.
 * Resolver forms index name from one or several parts where every part is a value of specified event tag.
 * Parts are joined with dash {@code '-'}. The index should satisfy Elastic restrictions.
 *
 * @author Petr Demenev
 */
public class TagsIndexResolver extends IndexResolver {

    private final List<IndexPart> indexParts;

    public TagsIndexResolver(Properties properties) {
        super(properties);
        String[] indexTags = PropertiesUtil.get(Props.INDEX_TAGS, properties).get();
        indexParts = new ArrayList<>(indexTags.length);
        for (String indexTag : indexTags) {
            boolean isOptional = indexTag.endsWith("?");
            HPath path = HPath.fromPath(isOptional ? indexTag.substring(0, indexTag.length() - 1) : indexTag);
            indexParts.add(new IndexPart(path, isOptional));
        }
    }

    @Override
    public Optional<String> resolve(Event event) {
        Container payload = event.getPayload();
        List<String> parts = new ArrayList<>(indexParts.size());
        for (IndexPart indexPart : indexParts) {
            Variant variant = indexPart.path.extract(payload);
            if ((variant == null || variant.getType() != Type.STRING)) {
                if (indexPart.optional) {
                    continue;
                }
                return Optional.empty();
            }
            parts.add(new String((byte[]) variant.getValue(), StandardCharsets.UTF_8));
        }
        String index = String.join("-", parts);
        return Optional.of(index);
    }

    private static class IndexPart {
        private final HPath path;
        private final boolean optional;

        IndexPart(HPath path, boolean optional) {
            this.path = path;
            this.optional = optional;
        }
    }

    static class Props {
        static final Parameter<String[]> INDEX_TAGS =
                Parameter.stringArrayParameter("tags").
                        withDefault(new String[0]).
                        build();
    }
}
