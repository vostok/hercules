package ru.kontur.vostok.hercules.util.routing;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;
import ru.kontur.vostok.hercules.util.routing.interpolation.Interpolator;

import java.nio.charset.StandardCharsets;

/**
 * Unit tests for {@link SentryDestination}.
 *
 * @author Aleksandr Yuferov
 */
public class SentryDestinationTest {
    @Test
    public void shouldCorrectlySerializeFromJsonUsingJackson() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        SentryDestination destination = new SentryDestination("my-org", "my-proj");

        String result = objectMapper.writeValueAsString(destination);

        Assert.assertEquals("{\"organization\":\"my-org\",\"project\":\"my-proj\"}", result);
    }

    @Test
    public void shouldCorrectlyDeserializeFromJsonUsingJackson() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        SentryDestination expected = new SentryDestination("my-org", "my-proj");
        byte[] serialized = "{\"organization\":\"my-org\",\"project\":\"my-proj\"}".getBytes(StandardCharsets.UTF_8);

        SentryDestination actual = objectMapper.readValue(serialized, SentryDestination.class);

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void shouldInterpolateOrganization() {
        SentryDestination destination = new SentryDestination("{tag:some-tag}", "my-proj");
        Interpolator interpolator = new Interpolator();
        Interpolator.Context context = new Interpolator.Context();

        context.add("tag", "some-tag", "my-org");
        SentryDestination result = destination.interpolate(interpolator, context);

        Assert.assertEquals("my-org", result.organization());
    }

    @Test
    public void shouldInterpolateProject() {
        SentryDestination destination = new SentryDestination("my-org", "{tag:some-tag}");
        Interpolator interpolator = new Interpolator();
        Interpolator.Context context = new Interpolator.Context();

        context.add("tag", "some-tag", "my-proj");
        SentryDestination result = destination.interpolate(interpolator, context);

        Assert.assertEquals("my-proj", result.project());
    }

    @Test
    public void shouldDoNothingWithNoWhereDestinationInSanitizeMethod() {
        SentryDestination destination = SentryDestination.toNowhere();

        Assert.assertSame(destination, destination.sanitize());
    }

    @Test
    public void shouldReplaceIllegalCharactersInSanitizeMethod() {
        SentryDestination destination = SentryDestination.of("Illegal^Characters09", "o-t_her:+chars")
                .sanitize();

        Assert.assertEquals("illegal_characters09", destination.organization());
        Assert.assertEquals("o-t_her__chars", destination.project());
    }

    @Test
    public void shouldCreateDefaultDestination() {
        Assert.assertSame(SentryDestination.byDefault(), SentryDestination.of("__default__", "__default__"));
    }

    @Test
    public void shouldCreateDefaultDestinationFromJson() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        byte[] serialized = "{\"organization\":\"__default__\",\"project\":\"__default__\"}".getBytes(StandardCharsets.UTF_8);

        SentryDestination actual = objectMapper.readValue(serialized, SentryDestination.class);

        Assert.assertSame(SentryDestination.byDefault(), actual);
    }

    @Test
    public void shouldCreateNowhereDestination() {
        Assert.assertSame(SentryDestination.toNowhere(), SentryDestination.of(null, null));
    }

    @Test
    public void shouldCreateNowhereDestinationFromJson() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        byte[] serialized = "{\"organization\": null,\"project\": null}".getBytes(StandardCharsets.UTF_8);

        SentryDestination actual = objectMapper.readValue(serialized, SentryDestination.class);

        Assert.assertSame(SentryDestination.toNowhere(), actual);
    }
}
