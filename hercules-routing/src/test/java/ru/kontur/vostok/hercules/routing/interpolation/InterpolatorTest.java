package ru.kontur.vostok.hercules.routing.interpolation;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.List;

/**
 * @author Aleksandr Yuferov
 */
@RunWith(Enclosed.class)
public class InterpolatorTest {
    @RunWith(Parameterized.class)
    public static class InterpolationTest {
        public final String applicationTagValue = "dagon";

        @Parameterized.Parameter
        public String templateString;

        @Parameterized.Parameter(1)
        public String expectedResult;

        @Parameterized.Parameters
        public static Object[][] parameters() {
            return new Object[][]{
                    {"", ""},
                    {"nothing", "nothing"},
                    {"{tag:properties/application}", "dagon"},
                    {"prefix_{tag:properties/application}", "prefix_dagon"},
                    {"{tag:properties/application}_suffix", "dagon_suffix"},
                    {"{tag:properties/application}_{tag:properties/application}", "dagon_dagon"},
                    {"{tag:unknownTag}", null},
            };
        }

        @Test
        public void test() {
            Interpolator interpolator = new Interpolator();

            Interpolator.Context context = interpolator.createContext()
                    .add("tag", "properties/application", applicationTagValue);
            String result = interpolator.interpolate(templateString, context);

            Assert.assertEquals(expectedResult, result);
        }
    }

    @RunWith(Parameterized.class)
    public static class ExtractorTest {
        @Parameterized.Parameter
        public String templateString;

        @Parameterized.Parameter(1)
        public List<InterpolationExpression> expectedResult;

        @Parameterized.Parameters
        public static Object[][] parameters() {
            List<InterpolationExpression> singleApplicationTag = List.of(
                    InterpolationExpression.of("tag", "properties/application")
            );
            return new Object[][]{
                    {"", List.of()},
                    {"nothing", List.of()},
                    {"{tag:properties/application}", singleApplicationTag},
                    {"prefix_{tag:properties/application}", singleApplicationTag},
                    {"{tag:properties/application}_suffix", singleApplicationTag},
                    {"{tag:properties/application}_{tag:properties/application}", List.of(
                            InterpolationExpression.of("tag", "properties/application"),
                            InterpolationExpression.of("tag", "properties/application")
                    )},
            };
        }

        @Test
        public void test() {
            Interpolator interpolator = new Interpolator();

            List<InterpolationExpression> result = interpolator.extractInterpolations(templateString);

            Assert.assertEquals(expectedResult, result);
        }
    }

    @RunWith(Parameterized.class)
    public static class IncorrectSyntaxTest {
        @Parameterized.Parameter
        public String templateString;

        @Parameterized.Parameter(1)
        public String expectedMessage;

        @Parameterized.Parameters
        public static Object[][] parameters() {
            return new Object[][]{
                    {"{variable}", "namespace delimiter not found"},
                    {"{:variable}", "namespace name not found"},
                    {"{namespace:}", "variable name not found"},
                    {"{tag:value", "interpolation not closed"},
                    {"{tag:{tag:someother}}", "nested interpolations are prohibited"}
            };
        }

        @Test
        public void test() {
            Interpolator interpolator = new Interpolator();
            Interpolator.Context context = interpolator.createContext();

            Assert.assertThrows(expectedMessage, IncorrectInterpolationSyntaxException.class,
                    () -> interpolator.interpolate(templateString, context));
        }
    }
}
