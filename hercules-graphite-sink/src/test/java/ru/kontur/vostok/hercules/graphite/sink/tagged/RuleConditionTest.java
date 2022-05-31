package ru.kontur.vostok.hercules.graphite.sink.tagged;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import ru.kontur.vostok.hercules.graphite.sink.common.PatternMatcher;

/**
 * @author Aleksandr Yuferov
 */
public class RuleConditionTest {
    @Test
    public void construct() {
        RuleCondition condition = RuleCondition.of(new Tag("tag", "*value*"), new int[] { 1 });

        Assert.assertEquals(1, condition.ruleIndices().length);
        Assert.assertEquals(1, condition.ruleIndices()[0]);
        Assert.assertEquals("tag", condition.tagKey());
    }

    @Test
    public void shouldDelegateTestMethodToMatcher() {
        PatternMatcher matcher = Mockito.mock(PatternMatcher.class);
        RuleCondition condition = new RuleCondition("tag", new int[] { 1 }, matcher);

        condition.test("value");

        Mockito.verify(matcher).test("value");
    }
}
