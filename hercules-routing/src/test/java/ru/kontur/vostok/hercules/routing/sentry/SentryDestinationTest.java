package ru.kontur.vostok.hercules.routing.sentry;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;
import ru.kontur.vostok.hercules.routing.interpolation.Interpolator;

import java.nio.charset.StandardCharsets;

/**
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
}
