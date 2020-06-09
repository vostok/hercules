package ru.kontur.vostok.hercules.graphite.sink.filter;

import ru.kontur.vostok.hercules.graphite.sink.acl.AccessControlEntry;
import ru.kontur.vostok.hercules.graphite.sink.acl.AccessControlList;
import ru.kontur.vostok.hercules.graphite.sink.acl.AclParser;
import ru.kontur.vostok.hercules.graphite.sink.acl.Statement;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.sink.filter.EventFilter;
import ru.kontur.vostok.hercules.util.parameter.Parameter;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;
import ru.kontur.vostok.hercules.util.validation.ValidationResult;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

/**
 * MetricAclEventFilter uses ACL mechanism for filter out metric by metric name pattern.
 * <p>
 * For ACL filter initialization two properties are needed:<br>
 * {@code acl.paths} is the path to access control list, if not specified then default value = "file://metrics.acl",<br>
 * and {@code acl.defaultStatements} - default {@link Statement} for ACL, if not specified then default value = {@link Statement#DENY}".
 *
 * @author Vladimir Tsypaev
 */
public class MetricAclEventFilter extends EventFilter {
    private final AccessControlList acl;

    public MetricAclEventFilter(Properties properties) {
        super(properties);

        List<AccessControlEntry> list;
        String path = PropertiesUtil.get(Props.ACL_PATH, properties).get().substring("file://".length());
        try (FileInputStream in = new FileInputStream(path)) {
            list = AclParser.parse(in);
        } catch (IOException ex) {
            throw new IllegalArgumentException(ex);
        }

        Statement defaultStatement = PropertiesUtil.get(Props.ACL_DEFAULT_STATEMENT, properties).get();
        this.acl = new AccessControlList(list, defaultStatement);
    }

    @Override
    public boolean test(Event event) {
        return acl.isPermit(event);
    }

    private static class Props {
        private static final Parameter<String> ACL_PATH = Parameter.stringParameter("acl.path").
                withDefault("file://metrics.acl").
                withValidator(v -> {
                    final String prefix = "file://";
                    if (v.startsWith(prefix)) {
                        return ValidationResult.ok();
                    }
                    return ValidationResult.error("Value should start with " + prefix);
                }).
                build();

        private static final Parameter<Statement> ACL_DEFAULT_STATEMENT =
                Parameter.enumParameter("acl.defaultStatement", Statement.class).
                        withDefault(Statement.DENY).
                        build();
    }
}
