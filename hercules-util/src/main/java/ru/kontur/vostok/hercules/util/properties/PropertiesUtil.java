package ru.kontur.vostok.hercules.util.properties;

import ru.kontur.vostok.hercules.util.ClassUtil;
import ru.kontur.vostok.hercules.util.parameter.Parameter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * @author Gregory Koshelev
 */
public final class PropertiesUtil {

    public static Properties ofScope(Properties properties, String scope) {
        return subProperties(properties, scope, '.');
    }

    public static Properties subProperties(Properties properties, String prefix, char delimiter) {
        Properties props = new Properties();
        int prefixLength = prefix.length();
        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            String name = ((String) entry.getKey());
            if (name.length() > prefixLength && name.startsWith(prefix) && name.charAt(prefixLength) == delimiter) {
                props.setProperty(name.substring(prefixLength + 1), (String) entry.getValue());
            }
        }
        return props;
    }

    public static String prettyView(final Properties properties) {
        final StringBuilder builder = new StringBuilder();

        properties.stringPropertyNames().stream()
                .sorted()
                .forEach(key -> {
                    builder.append("\n\t")
                            .append(key)
                            .append("=")
                            .append(properties.getProperty(key));
                });

        return builder.toString();
    }

    public static <T> Parameter<T>.ParameterValue get(Parameter<T> parameter, Properties properties) {
        String property = properties.getProperty(parameter.name());
        return parameter.from(property);
    }

    public static Properties copy(Properties properties) {
        Properties copy = new Properties();
        copy.putAll(properties);
        return copy;
    }

    private PropertiesUtil() {
        /* static class */
    }

    /**
     * Build list of parentClass instances from properties.
     * <p>
     * Properties for each class are defined under scope {@code N}, where {@code N} is position in the property {@code list}.
     * <p>
     * The property {@code N.class} should be defined with N - full class name.
     * <p>
     * Example:
     * <pre>{@code
     * 0.class=ru.kontur.vostok.hercules.sink.filter.BlacklistEventFilter
     * 0.props.paths=properties/project,properties/environment
     * 0.props.patterns=my_project:testing,my_project:staging
     *
     * 1.class=ru.kontur.vostok.hercules.sink.filter.WhitelistEventFilter
     * 1.props.paths=properties/project,properties/environment
     * 1.props.patterns=my_project:production
     * }</pre>
     * Here, there are two classes are defined.
     *
     * @param properties  properties for classes
     * @param parentClass class type for building.
     *                    Notes:
     *                    The property {@code class} should be inheritor from parentClass
     *                    and implement constructor with Properties param
     * @return list of parentClass instances
     */
    public static <T> List<T> createClassInstanceList(Properties properties, Class<T> parentClass) {
        Integer[] indexes = properties.keySet().stream()
                .map(key -> ((String) key).split("\\.")[0])
                .filter(segment -> segment.matches("^\\d+$"))
                .map(Integer::parseInt)
                .distinct()
                .sorted()
                .toArray(Integer[]::new);

        if (indexes.length == 0) {
            return Collections.emptyList();
        }

        List<T> instances = new ArrayList<>(indexes.length);
        for (int i : indexes) {
            Properties subProperties = PropertiesUtil.ofScope(properties, Integer.toString(i));
            instances.add(createClassInstance(subProperties, parentClass));
        }
        return instances;
    }

    /**
     * Build parentClass instance from properties.
     * <p>
     * The property {@code class} should be defined with full class name.
     * <p>
     * Properties for class are defined under scope {@code props}
     * <p>
     * Example:
     * <pre>{@code
     * class=ru.kontur.vostok.hercules.sink.filter.BlacklistEventFilter
     * props.paths=properties/project,properties/environment
     * props.patterns=my_project:testing,my_project:staging
     * }</pre>
     *
     * @param properties  properties for class
     * @param parentClass class type for building.
     *                    Notes:
     *                    The property {@code class} should be inheritor from parentClass
     *                    and implement constructor with Properties param
     * @return parentClass instance
     */
    public static <T> T createClassInstance(Properties properties, Class<T> parentClass) {
        String className = PropertiesUtil.get(Props.CLASS, properties).get();
        Properties classProperties = PropertiesUtil.ofScope(properties, "props");
        return ClassUtil.fromClass(
                className,
                parentClass,
                new Class<?>[]{Properties.class},
                new Object[]{classProperties});
    }

    private static class Props {
        static final Parameter<String> CLASS =
                Parameter.stringParameter("class").
                        required().
                        build();
    }
}
