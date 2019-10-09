package ru.kontur.vostok.hercules.util.text;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Gregory Koshelev
 */
public class StringUtilTest {
    @Test
    public void shouldSplitEmptyString() {
        Assert.assertEquals(0, StringUtil.split("", '/', true).length);

        Assert.assertArrayEquals(new String[]{""}, StringUtil.split("", '/', false));
    }

    @Test
    public void shouldSplitDelimiterString() {
        Assert.assertEquals(0, StringUtil.split("/", '/', true).length);
        Assert.assertEquals(0, StringUtil.split("//", '/', true).length);
        Assert.assertEquals(0, StringUtil.split("///", '/', true).length);

        Assert.assertArrayEquals(new String[]{"", ""}, StringUtil.split("/", '/', false));
        Assert.assertArrayEquals(new String[]{"", "", ""}, StringUtil.split("//", '/', false));
        Assert.assertArrayEquals(new String[]{"", "", "", ""}, StringUtil.split("///", '/', false));
    }

    @Test
    public void shouldSplitWithLeadingDelimiter() {
        Assert.assertArrayEquals(
                "Ignore leading delimiter",
                new String[]{"aaa", "bb", "c"},
                StringUtil.split("/aaa/bb/c", '/', true));

        Assert.assertArrayEquals(
                "Ignore multiple leading delimiters",
                new String[]{"aaa", "bb", "c"},
                StringUtil.split("///aaa/bb/c", '/', true));

        Assert.assertArrayEquals(
                "Do not ignore leading delimiter",
                new String[]{"", "aaa", "bb", "c"},
                StringUtil.split("/aaa/bb/c", '/', false));

        Assert.assertArrayEquals(
                "Do not ignore multiple leading delimiters",
                new String[]{"", "", "", "aaa", "bb", "c"},
                StringUtil.split("///aaa/bb/c", '/', false));
    }

    @Test
    public void shouldSplitWithTrailingDelimiter() {
        Assert.assertArrayEquals(
                "Ignore trailing delimiter",
                new String[]{"aaa", "bb", "c"},
                StringUtil.split("aaa/bb/c/", '/', true));

        Assert.assertArrayEquals(
                "Ignore multiple trailing delimiters",
                new String[]{"aaa", "bb", "c"},
                StringUtil.split("aaa/bb/c///", '/', true));

        Assert.assertArrayEquals(
                "Do not ignore trailing delimiter",
                new String[]{"aaa", "bb", "c", ""},
                StringUtil.split("aaa/bb/c/", '/', false));

        Assert.assertArrayEquals(
                "Do not ignore multiple trailing delimiters",
                new String[]{"aaa", "bb", "c", "", "", ""},
                StringUtil.split("aaa/bb/c///", '/', false));
    }

    @Test
    public void shouldSplitWithConnectedDelimiters() {
        Assert.assertArrayEquals(
                "Ignore connected delimiters",
                new String[]{"aaa", "bb", "c"},
                StringUtil.split("aaa//bb///c", '/', true));

        Assert.assertArrayEquals(
                "Do not ignore connected delimiters",
                new String[]{"aaa", "", "bb", "", "", "c"},
                StringUtil.split("aaa//bb///c", '/', false));
    }

    @Test
    public void shouldSplitWithAnyDelimiters() {
        Assert.assertArrayEquals(
                new String[]{"aaa", "bb", "c"},
                StringUtil.split("/aaa//bb///c////", '/', true));

        Assert.assertArrayEquals(
                new String[]{"", "aaa", "", "bb", "", "", "c", "", "", "", ""},
                StringUtil.split("/aaa//bb///c////", '/', false));
    }
}
