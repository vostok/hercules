package ru.kontur.vostok.hercules.util.text;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

/**
 * @author Gregory Koshelev
 */
@RunWith(Enclosed.class)
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

    /**
     * Tests of {@link StringUtil#sanitize} method.
     */
    @RunWith(Parameterized.class)
    public static class SanitizeTest {

        @Parameter(0)
        public String input;

        @Parameter(1)
        public String expectedOutput;

        @Test
        public void test() {
            String actualOutput = StringUtil.sanitize(input, ch -> ch != '$');

            Assert.assertEquals(expectedOutput, actualOutput);
        }

        @Parameters
        public static Object[][] parameters() {
            return new Object[][] {
                    { "foobar", "foobar" },
                    { "$foobar", "_foobar" },
                    { "foobar$", "foobar_" },
                    { "foo$bar", "foo_bar" },
                    { "foo$$$$bar", "foo____bar" },
                    { "$", "_" },
            };
        }
    }

    /**
     * Tests of {@link StringUtil#equalsIgnoreCase(CharSequence, CharSequence)} method.
     */
    @RunWith(Parameterized.class)
    public static class EqualsIgnoreCaseCharSeqTest {
        @Parameter(0)
        public String lhs;

        @Parameter(1)
        public String rhs;

        @Parameter(2)
        public boolean expectedOutput;

        @Test
        public void test() {
            boolean actualOutput = StringUtil.equalsIgnoreCase(lhs, rhs);

            Assert.assertEquals(expectedOutput, actualOutput);
        }

        @Parameters
        public static Object[][] parameters() {
            return new Object[][] {
                    { "F", "F", true },
                    { "f", "f", true },
                    { "f", "F", true },
                    { "f", "FF", false },
                    { "ff", "F", false },
                    { "foobar", "FooBar", true },
                    { "foobar", "FooBaz", false },
                    { null, "FooBaz", false },
                    { "foobar", null, false },
                    { null, null, true },
            };
        }
    }
}
