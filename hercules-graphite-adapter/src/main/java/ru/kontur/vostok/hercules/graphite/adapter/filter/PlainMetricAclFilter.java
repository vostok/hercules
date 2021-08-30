package ru.kontur.vostok.hercules.graphite.adapter.filter;

import org.jetbrains.annotations.NotNull;
import ru.kontur.vostok.hercules.graphite.adapter.metric.Metric;
import ru.kontur.vostok.hercules.util.parameter.Parameter;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;
import ru.kontur.vostok.hercules.util.text.AsciiString;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

/**
 * The filter accepts metrics which are allowed by ACL.
 * <p>
 * The ACL is defined for metric names. ACL has the following format:
 * <ul>
 *   <li>Each ACL entry is {@link Rule#PERMIT} or {@link Rule#DENY}.</li>
 *   <li>The metric name should be matched to the regular expression of the entry.</li>
 *   <li>ACL entries are applied one by one until one of them matches the metric name.</li>
 *   <li>The ACL has a default rule which is applied if no entries match.</li>
 *   <li>An ACL file can contain empty lines.</li>
 *   <li>An ACL fie can contain lines starting with {@code #} (a.k.a. commentaries).</li>
 * </ul>
 * Sample:
 * <pre>
 *     # Permits metric names starting with 'test.hercules.'
 *     PERMIT test\.hercules\..*
 *
 *     # Deny other metric names starting with 'test.'
 *     DENY test\..*
 * </pre>
 *
 * @author Gregory Koshelev
 */
public class PlainMetricAclFilter implements MetricFilter {
    private final List<PlainMetricAclEntry> entries;
    private final Rule defaultRule;

    public PlainMetricAclFilter(Properties properties) {
        entries = parseAclFile(PropertiesUtil.get(Props.FILE_PATH, properties).get().substring("file://".length()));
        defaultRule = PropertiesUtil.get(Props.DEFAULT_RULE, properties).get();
    }

    public boolean test(Metric metric) {
        CharSequence metricName = new AsciiString(metric.name());

        for (PlainMetricAclEntry entry : entries) {
            if (entry.pattern().matcher(metricName).matches()) {
                return entry.isPermit();
            }
        }
        return defaultRule == Rule.PERMIT;
    }

    private static List<PlainMetricAclEntry> parseAclFile(String file) {
        List<PlainMetricAclEntry> entries = new ArrayList<>();

        try (FileInputStream in = new FileInputStream(file);
             BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String entry;
            while ((entry = reader.readLine()) != null) {
                if (entry.isEmpty() || entry.startsWith("#")) {
                    continue;
                }
                entries.add(PlainMetricAclEntry.from(entry));
            }
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException(ex);
        }

        return entries;
    }

    private static class PlainMetricAclEntry {
        private final Rule rule;
        private final Pattern pattern;

        PlainMetricAclEntry(@NotNull Rule rule, @NotNull Pattern pattern) {
            this.rule = rule;
            this.pattern = pattern;
        }

        public boolean isPermit() {
            return rule == Rule.PERMIT;
        }

        public Pattern pattern() {
            return pattern;
        }

        static PlainMetricAclEntry from(String entry) {
            int index = entry.indexOf(' ');
            if (index == -1 || index == 0 || index == entry.length() - 1) {
                throw new IllegalArgumentException("ACL entry is malformed");
            }

            return new PlainMetricAclEntry(
                    Rule.valueOf(entry.substring(0, index)),
                    Pattern.compile(entry.substring(index + 1)));
        }
    }

    private enum Rule {
        PERMIT,
        DENY;
    }

    private static class Props {
        static Parameter<String> FILE_PATH =
                Parameter.stringParameter("file.path").
                        required().
                        build();

        static Parameter<Rule> DEFAULT_RULE =
                Parameter.enumParameter("default.rule", Rule.class).
                        withDefault(Rule.PERMIT).
                        build();
    }
}
