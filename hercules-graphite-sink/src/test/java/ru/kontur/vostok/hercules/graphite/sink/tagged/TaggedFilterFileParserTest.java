package ru.kontur.vostok.hercules.graphite.sink.tagged;

import org.junit.Assert;
import org.junit.Test;
import ru.kontur.vostok.hercules.configuration.Sources;
import ru.kontur.vostok.hercules.graphite.sink.tagged.TaggedFilterFileParser.ParseResult;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Aleksandr Yuferov
 */
public class TaggedFilterFileParserTest {
    private final TaggedFilterFileParser parser = new TaggedFilterFileParser();

    @Test
    public void shouldCorrectlyParseRulesWithSingleCondition() throws Exception {
        BufferedReader reader = new BufferedReader(new StringReader("tag1=value1"));

        ParseResult result = parser.parse(reader);

        Assert.assertEquals(1, result.getRulesCount());
        Assert.assertArrayEquals(new int[]{1}, result.getRuleConditionCount());
        List<RuleCondition> conditions = result.getConditions();
        Assert.assertEquals(1, conditions.size());
        RuleCondition createdCondition = conditions.get(0);
        Assert.assertArrayEquals(new int[]{0}, createdCondition.ruleIndices());
        Assert.assertEquals("tag1", createdCondition.tagKey());
    }

    @Test
    public void shouldCorrectlyParseRulesWithMultipleConditions() throws Exception {
        BufferedReader reader = new BufferedReader(new StringReader("tag1=value1;tag2=value2"));

        ParseResult result = parser.parse(reader);

        Assert.assertEquals(1, result.getRulesCount());
        Assert.assertArrayEquals(new int[]{2}, result.getRuleConditionCount());
        List<RuleCondition> conditions = result.getConditions();
        Assert.assertEquals(2, conditions.size());
        assertContains(conditions, cond -> cond.tagKey().equals("tag1") &&
                Arrays.equals(new int[]{0}, cond.ruleIndices()));
        assertContains(conditions, cond -> cond.tagKey().equals("tag2") &&
                Arrays.equals(new int[]{0}, cond.ruleIndices()));
    }

    @Test
    public void shouldJoinEquivalentConditions() throws Exception {
        BufferedReader reader = new BufferedReader(new StringReader(String.join("\n",
                "tag1=value1;tag2=value2",
                "tag1=value1;tag2=value3"
        )));

        ParseResult result = parser.parse(reader);

        Assert.assertEquals(2, result.getRulesCount());
        Assert.assertArrayEquals(new int[]{2, 2}, result.getRuleConditionCount());
        List<RuleCondition> conditions = result.getConditions();
        Assert.assertEquals(3, conditions.size());
        assertContains(conditions, cond -> cond.tagKey().equals("tag1") &&
                Arrays.equals(new int[]{0, 1}, cond.ruleIndices()));
        assertContains(conditions, cond -> cond.tagKey().equals("tag2") &&
                Arrays.equals(new int[]{0}, cond.ruleIndices()));
        assertContains(conditions, cond -> cond.tagKey().equals("tag2") &&
                Arrays.equals(new int[]{1}, cond.ruleIndices()));
    }

    @Test
    public void shouldSupportCommentsAndEmptyLines() throws Exception {
        BufferedReader reader = new BufferedReader(new StringReader(String.join("\n",
                "# my comment",
                "tag1=value1;tag2=value2",
                " ",
                "\t",
                "",
                "# tag3=value3;tag4=value4"

        )));

        ParseResult result = parser.parse(reader);

        Assert.assertEquals(1, result.getRulesCount());
    }

    @Test
    public void shouldThrowHumanReadableErrorIfParseExceptionOccurred() {
        BufferedReader reader = new BufferedReader(new StringReader("illegal:delimiter"));
        IllegalArgumentException exception = Assert
                .assertThrows(IllegalArgumentException.class, () -> parser.parse(reader));

        Assert.assertEquals(
                "an error occurred while parsing file line 1: missing '=' char in condition 'illegal:delimiter'",
                exception.getMessage()
        );
    }

    @Test
    public void shouldReadFromFileByPath() {
        ParseResult result = Sources.load("file://src/test/resources/filter-list.tfl", parser::parse);

        Assert.assertEquals(4, result.getConditions().size());
        Assert.assertEquals(2, result.getRulesCount());
        Assert.assertArrayEquals(new int[]{1, 3}, result.getRuleConditionCount());
    }

    static <T> void assertContains(List<T> list, Predicate<T> condition) {
        for (var elem : list) {
            if (condition.test(elem)) {
                return;
            }
        }
        Assert.fail("there is no element satisfies condition");
    }
}
