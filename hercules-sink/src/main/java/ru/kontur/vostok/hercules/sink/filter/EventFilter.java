package ru.kontur.vostok.hercules.sink.filter;

import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.util.parameter.Parameter;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
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
     * The property {@code list} should be defined with the list of filter full class names.
     * <p>
     * Note, filters are applied in order of definition in the property.
     * Properties for each filter are defined under scope {@code N}, where {@code N} is position in the property {@code list}.
     * <p>
     *     Sample:
     * <pre>{@code
     * list=ru.kontur.vostok.hercules.sink.filter.BlacklistEventFilter,ru.kontur.vostok.hercules.sink.filter.WhitelistEventFilter
     *
     * 0.paths=properties/project,properties/environment
     * 0.patterns=my_project:testing,my_project:staging
     *
     * 1.paths=properties/project,properties/environment
     * 1.patterns=my_project:production
     * }</pre>
     * Here, there are two filters are defined.
     *
     * @param properties properties for filters
     * @return list of filters
     */
    public static List<EventFilter> from(Properties properties) {
        String[] filterClasses = PropertiesUtil.get(Props.LIST, properties).get();

        if (filterClasses.length == 0) {
            return Collections.emptyList();
        }

        List<EventFilter> filters = new ArrayList<>(filterClasses.length);
        for (int i = 0; i < filterClasses.length; i++) {
            String filterClassName = filterClasses[i];

            Class<?> filterClass;
            try {
                filterClass = Class.forName(filterClassName);
            } catch (ClassNotFoundException ex) {
                throw new IllegalArgumentException("Unknown filter", ex);
            }
            if (!EventFilter.class.isAssignableFrom(filterClass)) {
                throw new IllegalArgumentException(filterClassName + " is not EventFilter");
            }

            Constructor<?> constructor;
            try {
                constructor = filterClass.getConstructor(Properties.class);
            } catch (NoSuchMethodException ex) {
                throw new IllegalArgumentException("Filter should ", ex);
            }

            Object filter;
            try {
                filter = constructor.newInstance(PropertiesUtil.ofScope(properties, Integer.toString(i)));
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException ex) {
                throw new IllegalArgumentException("Failed to init filter", ex);
            }
            filters.add((EventFilter) filter);
        }
        return filters;
    }

    private static class Props {
        static final Parameter<String[]> LIST =
                Parameter.stringArrayParameter("list").
                        withDefault(new String[0]).
                        build();
    }
}
