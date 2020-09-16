package ru.kontur.vostok.hercules.sentry.sink.filter;

import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.util.ContainerUtil;
import ru.kontur.vostok.hercules.tags.CommonTags;
import ru.kontur.vostok.hercules.sink.filter.EventFilter;
import ru.kontur.vostok.hercules.util.parameter.Parameter;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;

import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Sentry blacklist event filter uses patterns to filter out any events with tag values which match any of patterns.
 * <p>
 * For filter initialization one property is needed:<br>
 * {@code patterns} is the list of value patterns separated by the colon {@code :}.<br>
 * Patterns correspond following tag paths respectively:<br>
 * - {@code properties/project},<br>
 * - {@code properties/environment},<br>
 * - {@code properties/subproject}.<br>
 * <p>
 * Filter supports star {@code *} in the pattern definition. It means {@code any value}.
 * Also filter support empty value ({@code ""}) in the pattern definition. It means {@code empty} or {@code absent value}.
 * <p>
 * Sample:
 * <pre>patterns=my_project:testing:*,my_project:prod:</pre>
 * Here, following events will be filtered out by this filter:<br>
 * - events for project {@code my_project} from {@code testing} environment for any subproject;<br>
 * - events for project {@code my_project} from {@code prod} environment for absent subproject or subproject with empty value.
 *
 * @author Petr Demenev
 */
public class SentryBlacklistEventFilter extends EventFilter {
    private static final String STAR = "*";
    private static final int PATTERN_SIZE = 3;
    private static final Pattern FORBIDDEN_CHARS_PATTERN = Pattern.compile("[^-_a-zA-Z0-9]");
    private final List<String[]> patterns;

    public SentryBlacklistEventFilter(Properties properties) {
        super(properties);

        this.patterns = Stream.of(PropertiesUtil.get(Props.PATTERNS, properties).get()).
                map(x -> x.split(":", -1)).
                collect(Collectors.toList());

        for (String[] pattern : patterns) {
            if (PATTERN_SIZE != pattern.length) {
                throw new IllegalArgumentException("Pattern should contain " + PATTERN_SIZE + " values");
            }
        }
    }

    @Override
    public boolean test(Event event) {
        if (patterns.isEmpty()) {
            return true;
        }

        Optional<Container> propertiesOptional = ContainerUtil.extract(event.getPayload(), CommonTags.PROPERTIES_TAG);
        if (!propertiesOptional.isPresent()) {
            return true;
        }
        Container properties = propertiesOptional.get();
        String project = sanitize(ContainerUtil.extract(properties, CommonTags.PROJECT_TAG).orElse(null));
        String environment = sanitize(ContainerUtil.extract(properties, CommonTags.ENVIRONMENT_TAG).orElse(null));
        String subproject = sanitize(ContainerUtil.extract(properties, CommonTags.SUBPROJECT_TAG).orElse(null));

        for (String[] pattern : patterns) {
            if (matchValue(project, pattern[0])
                    && matchValue(environment, pattern[1])
                    && matchValue(subproject, pattern[2])) {
                return false;
            }
        }
        return true;
    }

    private static String sanitize(String value) {
        if (value == null) {
            return null;
        }
        return FORBIDDEN_CHARS_PATTERN.matcher(value).replaceAll("_");
    }

    private static boolean matchValue(String value, String patternElement) {
        if (patternElement.equals(STAR)) {
            return true;
        }
        if (value == null || value.isEmpty()) {
            return patternElement.isEmpty();
        }
        return value.equalsIgnoreCase(patternElement);
    }

    private static class Props {
        private static final Parameter<String[]> PATTERNS = Parameter.stringArrayParameter("patterns").
                withDefault(new String[0]).
                build();
    }
}
