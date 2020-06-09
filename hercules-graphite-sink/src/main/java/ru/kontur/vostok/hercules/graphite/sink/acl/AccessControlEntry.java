package ru.kontur.vostok.hercules.graphite.sink.acl;

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
    private final PatternSegment[] pattern;

    public AccessControlEntry(String ace) {
        String[] aceParts = ace.split("\\s", 2);
        this.statement = Statement.valueOf(aceParts[0]);
        this.pattern = Arrays.stream(aceParts[1].split("\\."))
                .map(PatternSegment::new)
                .toArray(PatternSegment[]::new);
    }

    public boolean isPermit() {
        return statement.isPermit();
    }

    public PatternSegment[] getPattern() {
        return pattern;
    }
}
