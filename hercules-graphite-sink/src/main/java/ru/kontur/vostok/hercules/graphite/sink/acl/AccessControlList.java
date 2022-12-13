package ru.kontur.vostok.hercules.graphite.sink.acl;

import ru.kontur.vostok.hercules.graphite.sink.common.PatternMatcher;
import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.util.ContainerUtil;
import ru.kontur.vostok.hercules.tags.MetricsTags;

import java.util.List;

/**
 * Class for representing access control list (ACL).
 * <p>
 * Matches metric names to patterns of ACL entries sequentially from top to bottom
 * and returns {@link Statement} operation of the first matched entry.<br>
 * If the metric name doesn't fit any pattern in the ACL, then returns {@code acl.defaultStatements} operation.
 * <p>
 * ACL filter supports {@code *} in the pattern definition, means {@code any value}.<br>
 *
 * @author Vladimir Tsypaev
 */
public class AccessControlList {
    private final List<AccessControlEntry> list;
    private final boolean defaultAnswer;
    private final int maxPatternLength;

    public AccessControlList(List<AccessControlEntry> list, Statement defaultStatement) {
        this.list = list;
        this.defaultAnswer = defaultStatement == Statement.PERMIT;
        this.maxPatternLength = list.stream().mapToInt(it -> it.getPattern().length).max().orElse(0);
    }

    /**
     * Checks which rule this event fits
     *
     * @param event metric event
     * @return {@code true} if event should pass filter, otherwise {@code false}
     */
    public boolean isPermit(Event event) {
        if (list.isEmpty()) {
            return defaultAnswer;
        }
        Container[] tagsVector = ContainerUtil
                .extract(event.getPayload(), MetricsTags.TAGS_VECTOR_TAG)
                .orElse(null);
        if (tagsVector == null) {
            return defaultAnswer;
        }

        int maxRequiredValue = Math.min(tagsVector.length, maxPatternLength);
        String[] tagsValues = new String[maxRequiredValue];

        for (int i = 0; i < maxRequiredValue; i++) {
            // TODO: ContainerUtil.extract is expensive!
            //  (each time detecting extractor and coping values in StandardExtractors.extractString)
            tagsValues[i] = ContainerUtil.extract(tagsVector[i], MetricsTags.TAG_VALUE_TAG).orElse("null");
        }

        for (AccessControlEntry ace : list) {
            PatternMatcher[] pattern = ace.getPattern();
            if (matchingIsPossible(tagsVector, pattern)) {
                for (int i = 0; i < tagsValues.length; i++) {
                    if (!pattern[i].test(tagsValues[i])) {
                        break;
                    }
                    if (pattern.length == i + 1) {
                        return ace.isPermit();
                    }
                }
            }
        }
        return defaultAnswer;
    }

    /**
     * Checks that value can match the pattern
     *
     * @param tagsVector array of metric name segments
     * @param pattern    array with pattern segments
     * @return {@code true} if matching is possible, otherwise {@code false}
     */
    private boolean matchingIsPossible(Container[] tagsVector, PatternMatcher[] pattern) {
        if (pattern[pattern.length - 1].isAny()) {
            return tagsVector.length >= pattern.length;
        }
        return tagsVector.length == pattern.length;
    }
}
