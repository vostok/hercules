package ru.kontur.vostok.hercules.sink.filter;

import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.Type;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.protocol.hpath.HPath;
import ru.kontur.vostok.hercules.util.parameter.Parameter;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;

import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Blacklist filter uses paths and corresponding patterns to filter out any events with tag values from paths are same as any of patterns has.
 * <p>
 * For blacklist filter initialization two properties are needed:<br>
 * {@code paths} is the list of {@link HPath} in the string-path form ({@code paths} can be empty),<br>
 * and {@code patterns} is the list of value patterns for tags from paths above, values in the pattern are separated by the colon {@code :} ({@code patterns} can be empty).
 * <p>
 * Blacklist filter supports star {@code *} in the pattern definition. It means {@code any value}.
 * <p>
 * FIXME: Currently, this filter supports only tags of type {@link Type#STRING} and has inefficient walkthrough over patterns when test events.
 * <p>
 * Sample:
 * <pre>{@code paths=properties/project,properties/environment
 * patterns=my_project:testing,my_project:staging}</pre>
 * Here, events for project {@code my_project} from {@code testing} and {@code staging} environments will be filtered out.
 * @author Gregory Koshelev
 */
public class BlacklistEventFilter extends EventFilter {

    private final List<HPath> paths;
    private final PatternTree blacklistTree;

    /**
     * Inheritors must implement constructor with the same signature.
     *
     * @param properties properties for the filter initialization
     */
    public BlacklistEventFilter(Properties properties) {
        super(properties);

        this.paths = Stream.of(PropertiesUtil.get(Props.PATHS, properties).get()).
                map(HPath::fromPath).
                collect(Collectors.toList());

        this.blacklistTree = new PatternTree(Collections.nCopies(paths.size(), Type.STRING));

        for (String pattern : PropertiesUtil.get(Props.PATTERNS, properties).get()) {
            blacklistTree.put(pattern);
        }
    }

    @Override
    public boolean test(Event event) {
        if (blacklistTree.isEmpty()) {
            return true;
        }

        Container payload = event.getPayload();
        List<Variant> variants = paths.stream().
                map(path -> path.extract(payload)).
                collect(Collectors.toList());

        return !blacklistTree.matches(variants);
    }

    private static class Props {
        private static final Parameter<String[]> PATHS = Parameter.stringArrayParameter("paths").
                withDefault(new String[0]).
                build();

        private static final Parameter<String[]> PATTERNS = Parameter.stringArrayParameter("patterns").
                withDefault(new String[0]).
                build();
    }
}
