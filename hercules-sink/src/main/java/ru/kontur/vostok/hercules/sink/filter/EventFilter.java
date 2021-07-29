package ru.kontur.vostok.hercules.sink.filter;

import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;

import java.util.List;
import java.util.Properties;

/**
 * Check if the event pass the filter.
 * <p>
 * Inheritors must implement constructors with the same signature as {@link EventFilter} itself.
 *
 * @author Gregory Koshelev
 */
public abstract class EventFilter {
    /**
     * Inheritors must implement constructor with the same signature.
     *
     * @param properties properties for the filter initialization
     */
    public EventFilter(Properties properties) {
    }

    /**
     * Check if the event pass the filter.
     *
     * @param event the event to check
     * @return {@code true} if the event pass the filter, otherwise {@code false}
     */
    public abstract boolean test(Event event);

    /**
     * Build list of filters from properties.
     * <p>
     * The property {@code N.class} should be defined with N-filter full class name.
     * <p>
     * Note, filters are applied in order of definition in the property.
     * Properties for each filter are defined under scope {@code N}, where {@code N} is position in the property {@code list}.
     * <p>
     *     Sample:
     * <pre>{@code
     * 0.class=ru.kontur.vostok.hercules.sink.filter.BlacklistEventFilter
     * 0.props.paths=properties/project,properties/environment
     * 0.props.patterns=my_project:testing,my_project:staging
     *
     * 1.class=ru.kontur.vostok.hercules.sink.filter.WhitelistEventFilter
     * 1.props.paths=properties/project,properties/environment
     * 1.props.patterns=my_project:production
     * }</pre>
     * Here, there are two filters are defined.
     *
     * @param properties properties for filters
     * @return list of filters
     */
    public static List<EventFilter> from(Properties properties) {
        return PropertiesUtil.listFromProperties(properties, EventFilter.class);
    }
}
