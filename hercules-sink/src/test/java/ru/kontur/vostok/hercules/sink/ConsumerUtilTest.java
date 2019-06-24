package ru.kontur.vostok.hercules.sink;

import org.junit.Assert;
import org.junit.Test;
import ru.kontur.vostok.hercules.util.PatternMatcher;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author Gregory Koshelev
 */
public class ConsumerUtilTest {
    @Test
    public void shouldBuildGroupIdBySinglePattern() {
        List<PatternMatcher> patternMatchers = Collections.singletonList(new PatternMatcher("legacy_logs_elk_c2"));
        Assert.assertEquals(
                "hercules.test.legacy_logs_elk_c2",
                ConsumerUtil.toGroupId("test", patternMatchers));
    }

    @Test
    public void shouldBuildGroupIdByListOfPatterns() {
        List<PatternMatcher> patternMatchers =
                Arrays.asList(new PatternMatcher("legacy_logs_elk_c2"), new PatternMatcher("legacy_logs_elk_c1"));
        Assert.assertEquals(
                "hercules.test.legacy_logs_elk_c2,legacy_logs_elk_c1",
                ConsumerUtil.toGroupId("test", patternMatchers));
    }
}
