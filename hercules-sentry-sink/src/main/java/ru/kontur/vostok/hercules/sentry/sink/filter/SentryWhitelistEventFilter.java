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
 * Sentry Whitelist event filter uses patterns to filter out any events with tag values which don't match any of patterns.
 * <p>
 * For filter initialization one property is needed:<br>
 * {@code patterns} is the list of value patterns separated by the colon {@code :}.<br>
 * Patterns correspond following tag paths respectively:<br>
 * - {@code properties/project},<br>
 * - {@code properties/environment},<br>
 * - {@code properties/subproject}.<br>
 * <p>
 * Filter supports star {@code *} in the pattern definition. It means {@code any value}.
 * <p>
 * Sample:
 * <pre>patterns=my_project:prod:*,my_project:testing:my_subproject</pre>
 * Here, events for project {@code my_project} from {@code prod} environment for any subproject and
 * events for project {@code my_project} from {@code testing} environment for subproject {@code my_subproject}
 * will be passed through the filter.
 *
 * @author Petr Demenev
 */
public class SentryWhitelistEventFilter extends EventFilter {
    private static final String STAR = "*";
    private static final int SIZE = 3;
    private static final Pattern FORBIDDEN_CHARS_PATTERN = Pattern.compile("[^-_a-zA-Z0-9]");
    private final List<String[]> patterns;

    public SentryWhitelistEventFilter(Properties properties) {
        super(properties);

        this.patterns = Stream.of(PropertiesUtil.get(Props.PATTERNS, properties).get()).
                map(x -> Stream.of(x.split(":")).
                        toArray(String[]::new)).
                collect(Collectors.toList());

        for (String[] pattern : patterns) {
            if (SIZE != pattern.length) {
                throw new IllegalArgumentException("Pattern length should be equal to " + SIZE);
            }
        }
    }

    @Override
    public boolean test(Event event) {
        if (patterns.isEmpty()) {
            return false;
        }

        Optional<Container> propertiesOptional = ContainerUtil.extract(event.getPayload(), CommonTags.PROPERTIES_TAG);
        if (!propertiesOptional.isPresent()) {
            return false;
        }
        Container properties = propertiesOptional.get();
        String project = sanitize(ContainerUtil.extract(properties, CommonTags.PROJECT_TAG).orElse(null));
        String environment = sanitize(ContainerUtil.extract(properties, CommonTags.ENVIRONMENT_TAG).orElse(null));
        String subproject = sanitize(ContainerUtil.extract(properties, CommonTags.SUBPROJECT_TAG).orElse(null));

        for (String[] pattern : patterns) {
            if (testValue(project, pattern[0])
                    && testValue(environment, pattern[1])
                    && testValue(subproject, pattern[2])) {
                return true;
            }
        }
        return false;
    }

    private static String sanitize(String value) {
        if (value == null) {
            return null;
        }
        return FORBIDDEN_CHARS_PATTERN.matcher(value).replaceAll("_");
    }

    private static boolean testValue(String value, String patternElement) {
        if (patternElement.equals(STAR)) {
            return true;
        }
        if (value == null) {
            return false;
        }
        return value.equalsIgnoreCase(patternElement);
    }

    private static class Props {
        private static final Parameter<String[]> PATTERNS = Parameter.stringArrayParameter("patterns").
                withDefault(new String[0]).
                build();
    }
}
