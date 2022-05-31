package ru.kontur.vostok.hercules.graphite.sink.acl;

import ru.kontur.vostok.hercules.graphite.sink.common.PatternMatcher;

import java.util.Arrays;

/**
 * Class for representing ACL rule.<br>
 * statement - action applied to the metric event ({@link Statement#DENY} or {@link Statement#PERMIT});<br>
 * pattern - pattern for matching metric name. The pattern looks like an array of dotted metric name segments.
 *
 * @author Vladimir Tsypaev
 */
public class AccessControlEntry {
    private final Statement statement;
    private final PatternMatcher[] pattern;

    static AccessControlEntry fromString(String ace) {
        String[] aceParts = ace.split("\\s", 2);
        Statement statement = Statement.valueOf(aceParts[0]);
        PatternMatcher[] pattern = Arrays.stream(aceParts[1].split("\\."))
                .map(PatternMatcher::of)
                .toArray(PatternMatcher[]::new);
        return new AccessControlEntry(statement, pattern);
    }

    private AccessControlEntry(Statement statement, PatternMatcher[] pattern) {
        this.statement = statement;
        this.pattern = pattern;
    }

    boolean isPermit() {
        return statement == Statement.PERMIT;
    }

    PatternMatcher[] getPattern() {
        return pattern;
    }
}
