package ru.kontur.vostok.hercules.graphite.sink.common;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Aleksandr Yuferov
 */
public class PatternMatcherTest {
    @Test
    public void shouldConstructAnyMatcher() {
        Assert.assertEquals(PatternMatcher.AnyMatcher.class, PatternMatcher.of("*").getClass());
    }

    @Test
    public void shouldConstructPrefixMatcher() {
        Assert.assertEquals(PatternMatcher.PrefixMatcher.class, PatternMatcher.of("foo*").getClass());
    }

    @Test
    public void shouldConstructSuffixMatcher() {
        Assert.assertEquals(PatternMatcher.SuffixMatcher.class, PatternMatcher.of("*foo").getClass());
    }

    @Test
    public void shouldConstructPrefixAndSuffixMatcher() {
        Assert.assertEquals(PatternMatcher.PrefixAndSuffixMatcher.class, PatternMatcher.of("foo*bar").getClass());
    }

    @Test
    public void shouldConstructContainsMatcher() {
        Assert.assertEquals(PatternMatcher.ContainsMatcher.class, PatternMatcher.of("*foo*").getClass());
    }

    @Test
    public void shouldFallbackToEqualsMatcher() {
        Assert.assertEquals(PatternMatcher.EqualsMatcher.class, PatternMatcher.of("asd*asd*asd").getClass());
    }

    @Test
    public void shouldConstructEqualsMatcher() {
        Assert.assertEquals(PatternMatcher.EqualsMatcher.class, PatternMatcher.of("foo").getClass());
    }

    @Test
    public void anyMatcherShouldReturnTrueInAnyCase() {
        PatternMatcher matcher = PatternMatcher.of("*");

        Assert.assertTrue(matcher.test(null));
    }

    @Test
    public void prefixMatcherWorksProperly() {
        PatternMatcher matcher = PatternMatcher.of("prefix-*");

        Assert.assertTrue(matcher.test("prefix-value"));
        Assert.assertTrue(matcher.test("prefix-"));
        Assert.assertFalse(matcher.test("value"));
    }

    @Test
    public void suffixMatcherWorksProperly() {
        PatternMatcher matcher = PatternMatcher.of("*-suffix");

        Assert.assertTrue(matcher.test("value-suffix"));
        Assert.assertTrue(matcher.test("-suffix"));
        Assert.assertFalse(matcher.test("value"));
    }

    @Test
    public void prefixAndSuffixMatcherWorksProperly() {
        PatternMatcher matcher = PatternMatcher.of("prefix-*-suffix");

        Assert.assertTrue(matcher.test("prefix-value-suffix"));
        Assert.assertTrue(matcher.test("prefix--suffix"));
        Assert.assertFalse(matcher.test("value-suffix"));
        Assert.assertFalse(matcher.test("prefix-value"));
        Assert.assertFalse(matcher.test("value"));
    }

    @Test
    public void prefixAndSuffixMatcherWorksProperlyWithPalindromes() {
        PatternMatcher matcher = PatternMatcher.of("olo*lo");

        Assert.assertTrue(matcher.test("ololo"));
        Assert.assertTrue(matcher.test("oloolo"));
        Assert.assertFalse(matcher.test("olo"));
    }

    @Test
    public void containsMatcherWorksProperly() {
        PatternMatcher matcher = PatternMatcher.of("*contains*");

        Assert.assertTrue(matcher.test("contains"));
        Assert.assertTrue(matcher.test("prefix-contains"));
        Assert.assertTrue(matcher.test("contains-suffix"));
        Assert.assertTrue(matcher.test("prefix-contains-suffix"));
        Assert.assertFalse(matcher.test("value"));
    }

    @Test
    public void equalsMatcherWorksProperly() {
        PatternMatcher matcher = PatternMatcher.of("value");

        Assert.assertTrue(matcher.test("value"));
        Assert.assertFalse(matcher.test("other"));
    }
}
