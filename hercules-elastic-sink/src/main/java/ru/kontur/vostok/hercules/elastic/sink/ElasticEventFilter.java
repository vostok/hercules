package ru.kontur.vostok.hercules.elastic.sink;

import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.util.ContainerUtil;
import ru.kontur.vostok.hercules.sink.filter.EventFilter;
import ru.kontur.vostok.hercules.tags.CommonTags;
import ru.kontur.vostok.hercules.tags.ElasticSearchTags;
import ru.kontur.vostok.hercules.util.text.CharUtil;

import java.util.Optional;
import java.util.Properties;

/**
 * Tags
 * {@code properties/elk-index},
 * {@code properties/project},
 * {@code properties/environment}
 * and {@code properties/subproject}
 * are used in {@link ru.kontur.vostok.hercules.elastic.sink.index.IndexResolver} to determine index name.
 * Thus,<br>
 * 1. {@link CommonTags#PROJECT_TAG project} must be specified if {@link ElasticSearchTags#ELK_INDEX_TAG elk-index} is absent.<br>
 * 2. All tags from above must start with alphanumeric (latin or digit characters) if present.
 * <p>
 * It is important to note, that tag values should be sanitized in {@link ru.kontur.vostok.hercules.elastic.sink.index.IndexResolver} anyway.
 * See its implementation.
 *
 * @author Gregory Koshelev
 */
public class ElasticEventFilter extends EventFilter {
    ElasticEventFilter(Properties properties) {
        super(properties);
    }

    @Override
    public boolean test(Event event) {
        Optional<Container> properties = ContainerUtil.extract(event.getPayload(), CommonTags.PROPERTIES_TAG);
        if (!properties.isPresent()) {
            return false;
        }

        Optional<String> index = ContainerUtil.extract(properties.get(), ElasticSearchTags.ELK_INDEX_TAG);
        if (index.isPresent()) {
            return test(index.get());
        }

        Optional<String> project = ContainerUtil.extract(properties.get(), CommonTags.PROJECT_TAG);
        if (!project.isPresent() || !test(project.get())) {
            return false;
        }

        Optional<String> environment = ContainerUtil.extract(properties.get(), CommonTags.ENVIRONMENT_TAG);
        if (environment.isPresent() && !test(environment.get())) {
            return false;
        }

        Optional<String> subproject = ContainerUtil.extract(properties.get(), CommonTags.SUBPROJECT_TAG);
        if (subproject.isPresent() && !test(subproject.get())) {
            return false;
        }

        return true;
    }

    private boolean test(String s) {
        return !s.isEmpty() && CharUtil.isAlphaNumeric(s.charAt(0));

    }
}
