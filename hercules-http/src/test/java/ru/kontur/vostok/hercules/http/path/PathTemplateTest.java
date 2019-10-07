package ru.kontur.vostok.hercules.http.path;

import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;

/**
 * @author Gregory Koshelev
 */
public class PathTemplateTest {
    @Test
    public void shouldMatchRoot() {
        PathTemplate root = PathTemplate.of("/");
        Assert.assertTrue(root.isExactPath());

        PathTemplate.ExactPathMatcher matcher = root.toExactMatcher();
        Assert.assertTrue(matcher.match("/"));
        Assert.assertFalse(matcher.match("/aaa"));
        Assert.assertFalse(matcher.match("/aaa/bb"));
    }

    @Test
    public void shouldMatchExactPath() {
        PathTemplate path = PathTemplate.of("/aaa/bb/c");
        Assert.assertTrue(path.isExactPath());

        PathTemplate.ExactPathMatcher matcher = path.toExactMatcher();
        Assert.assertTrue(matcher.match("/aaa/bb/c"));
        Assert.assertFalse(matcher.match("/aaa/bb/c/"));//TODO: should ignore trailing slashes
        Assert.assertFalse(matcher.match("/aaa/bb"));
        Assert.assertFalse(matcher.match("/aaa"));
        Assert.assertFalse(matcher.match("/"));
    }

    @Test
    public void shouldMatchShortPathWithSingleParameter() {
        PathTemplate path = PathTemplate.of("/:a");
        Assert.assertFalse(path.isExactPath());

        PathTemplate.PathTemplateMatcher matcher = path.toMatcher();

        Assert.assertEquals(Collections.singletonMap("a", "aaa"), matcher.match(Path.of("/aaa")));
        Assert.assertEquals(Collections.singletonMap("a", "aaa"), matcher.match(Path.of("/aaa/")));
        Assert.assertEquals(Collections.emptyMap(), matcher.match(Path.of("/aaa/bb")));
        Assert.assertEquals(Collections.emptyMap(), matcher.match(Path.of("/aaa/bb/c")));
    }

    @Test
    public void shouldMatchLongPathWithSingleParameter() {
        PathTemplate path = PathTemplate.of("/aaa/:b/c");
        Assert.assertFalse(path.isExactPath());

        PathTemplate.PathTemplateMatcher matcher = path.toMatcher();

        Assert.assertEquals(Collections.singletonMap("b", "bb"), matcher.match(Path.of("/aaa/bb/c")));
        Assert.assertEquals(Collections.singletonMap("b", "bb"), matcher.match(Path.of("/aaa/bb/c/")));
        Assert.assertEquals(Collections.emptyMap(), matcher.match(Path.of("/aaa/bb")));
        Assert.assertEquals(Collections.emptyMap(), matcher.match(Path.of("/")));
    }

    @Test
    public void shouldMatchMultipleParameters() {
        PathTemplate path = PathTemplate.of("/:a/bb/:ccc");
        Assert.assertFalse(path.isExactPath());

        PathTemplate.PathTemplateMatcher matcher = path.toMatcher();

        Assert.assertEquals(
                new HashMap<String, String>() {{
                    put("a", "aaa");
                    put("ccc", "c");
                }},
                matcher.match(Path.of("/aaa/bb/c")));
        Assert.assertEquals(
                new HashMap<String, String>() {{
                    put("a", "aaa");
                    put("ccc", "c");
                }},
                matcher.match(Path.of("/aaa/bb/c/")));

        Assert.assertEquals(Collections.emptyMap(), matcher.match(Path.of("/aaa/bb/")));
        Assert.assertEquals(Collections.emptyMap(), matcher.match(Path.of("/aaa/bb")));
        Assert.assertEquals(Collections.emptyMap(), matcher.match(Path.of("//bb/c")));
    }
}
