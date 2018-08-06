import java.io.IOException;
import java.nio.file.Paths;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import ru.kontur.vostok.hercules.micrometer.registry.HerculesMetricConfig;
import ru.kontur.vostok.hercules.micrometer.registry.HerculesMetricConfigFactory;

/**
 * @author Daniil Zhenikhov
 */
public class HerculesMetricConfigFactoryTests {
    private static final String RESOURCES_PATH = Paths
            .get(System.getProperty("user.dir") ,"src", "test", "resources")
            .toString();
    private static final String FILENAME = "hercules-metric.properties";
    private static final String FULL_PATH = RESOURCES_PATH + "/" + FILENAME;
    private static final String OK_MESSAGE = "OK";
    private static final String TEST_KEY = "test";

    @BeforeClass
    public static void setUp() {
        System.setProperty("hercules.metric.config", FULL_PATH);
    }

    @Test
    public void shouldLoadResource() throws IOException {
        HerculesMetricConfig config = HerculesMetricConfigFactory.fromResource();

        Assert.assertEquals(OK_MESSAGE, config.get(TEST_KEY));
    }

    @Test
    public void shouldLoadSpecificResource() throws IOException {
        HerculesMetricConfig config = HerculesMetricConfigFactory.fromResource(FILENAME);

        Assert.assertEquals(OK_MESSAGE, config.get(TEST_KEY));
    }

    @Test
    public void shouldLoadFile() throws IOException {
        HerculesMetricConfig config = HerculesMetricConfigFactory.fromFile();

        Assert.assertEquals(OK_MESSAGE, config.get(TEST_KEY));
    }

    @Test
    public void shouldLoadSpecificFile() throws IOException {
        HerculesMetricConfig config = HerculesMetricConfigFactory.fromFile(FULL_PATH);

        Assert.assertEquals(OK_MESSAGE, config.get(TEST_KEY));
    }
}
