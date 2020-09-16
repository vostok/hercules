package ru.kontur.vostok.hercules.graphite.sink.acl;

import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.util.ContainerUtil;
import ru.kontur.vostok.hercules.tags.MetricsTags;

import java.util.List;
import java.util.Optional;

/**
 * Class for representing ACL.
 * <p>
 * ACL file is scanned from top to bottom and when the metric name is fully matched to pattern, the {@link Statement} for this pattern is returned.<br>
 * If metric name do not fit any of the patterns in the ACL file, then will return {@code acl.defaultStatements}.
 * <p>
 * ACL filter supports {@code *} in the pattern definition, means {@code any value}.<br>
 *
 * @author Vladimir Tsypaev
 */
public class AccessControlList {
    private final List<AccessControlEntry> list;
    private final Statement defaultStatement;

    public AccessControlList(List<AccessControlEntry> list, Statement defaultStatement) {
        this.list = list;
        this.defaultStatement = defaultStatement;
    }

    /**
     * Checks which rule this event fits
     *
     * @param event metric event
     * @return {@code true} if event should pass filter, otherwise {@code false}
     */
    public boolean isPermit(Event event) {
        if (list.isEmpty()) {
            return defaultStatement.isPermit();
        }

        Optional<Container[]> optionalTagsVector = ContainerUtil.extract(event.getPayload(), MetricsTags.TAGS_VECTOR_TAG);
        if (!optionalTagsVector.isPresent()) {
            return defaultStatement.isPermit();
        }

        Container[] tagsVector = optionalTagsVector.get();

        for (AccessControlEntry ace : list) {
            PatternSegment[] pattern = ace.getPattern();

            if (!matchingIsPossible(tagsVector, pattern)) {
                continue;
            }

            for (int i = 0; i < tagsVector.length; i++) {
                String value = ContainerUtil.extract(tagsVector[i], MetricsTags.TAG_VALUE_TAG).orElse("null");
                PatternSegment patternSegment = pattern[i];

                if (matches(value, patternSegment)) {
                    if (pattern.length == i + 1) {
                        return ace.isPermit();
                    }
                } else {
                    break;
                }
            }
        }
        return defaultStatement.isPermit();
    }

    /**
     * Checks that value can matches the pattern
     *
     * @param tagsVector array of metric name segments
     * @param pattern array with pattern segments
     * @return {@code true} if matching is possible, otherwise {@code false}
     */
    private boolean matchingIsPossible(Container[] tagsVector, PatternSegment[] pattern) {
        if (pattern[pattern.length - 1].isStar()) {
            return tagsVector.length >= pattern.length;
        }
        return tagsVector.length == pattern.length;
    }

    /**
     * Checks that pattern segment matches the value
     *
     * @param value metric name segment
     * @param segment pattern segment
     * @return {@code true} if it matches, otherwise {@code false}
     */
    private static boolean matches(String value, PatternSegment segment) {
        return (segment.containsStar())
                ? starMatches(value, segment)
                : segment.getSegment().equals(value);
    }

    /**
     * Checks the pattern segment which contains star matches the value
     *
     * @param value metric name segment
     * @param segment pattern segment
     * @return {@code true} if it matches, otherwise {@code false}
     */
    private static boolean starMatches(String value, PatternSegment segment) {
        if (segment.isStar()) {
            return true;
        }

        if (segment.startsWithStar()) {
            return value.endsWith(segment.getSuffix());
        }

        if (segment.endsWithStar()) {
            return value.startsWith(segment.getPrefix());
        }

        return value.startsWith(segment.getPrefix())
                && value.endsWith(segment.getSuffix());
    }
}
