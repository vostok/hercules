package ru.kontur.vostok.hercules.sentry.sink.client;

import io.sentry.dsn.Dsn;
import org.junit.Assert;
import org.junit.Test;

import java.util.Properties;
import ru.kontur.vostok.hercules.sentry.client.impl.v9.connector.HerculesClientFactory;

/**
 * @author Petr Demenev
 */
public class HerculesClientFactoryTest {

    private static final String dsnUri = "https://1234567813ef4c6ca4fbabc4b8f8cb7d@mysentry.io/1000001" +
            "?uncaught.handler.enabled=false&stacktrace.app.packages=%20";
    private static final Dsn sourceDsn = new Dsn(dsnUri);

    @Test
    public void shouldReplaceUrlTest() {
        Properties properties = new Properties();
        properties.setProperty("sentry.rewritingUrl", "http://localhost:8080");
        HerculesClientFactory herculesConnectorFactory = new HerculesClientFactory(properties);

        Dsn newDsn = herculesConnectorFactory.modifyDsn(sourceDsn);

        Assert.assertEquals("http", newDsn.getProtocol());
        Assert.assertEquals("localhost", newDsn.getHost());
        Assert.assertEquals(8080, newDsn.getPort());
        checkOtherFields(newDsn);
    }

    @Test
    public void shouldReplaceUrlWithoutPortTest() {
        Properties properties = new Properties();
        properties.setProperty("sentry.rewritingUrl", "http://example.com");
        HerculesClientFactory herculesClientFactory = new HerculesClientFactory(properties);

        Dsn newDsn = herculesClientFactory.modifyDsn(sourceDsn);

        Assert.assertEquals("http", newDsn.getProtocol());
        Assert.assertEquals("example.com", newDsn.getHost());
        Assert.assertEquals(-1, newDsn.getPort());
        checkOtherFields(newDsn);
    }

    @Test
    public void shouldSaveAllFieldsTest() {
        HerculesClientFactory herculesClientFactory = new HerculesClientFactory(new Properties());

        Dsn newDsn = herculesClientFactory.modifyDsn(sourceDsn);

        Assert.assertEquals("https", newDsn.getProtocol());
        Assert.assertEquals("mysentry.io", newDsn.getHost());
        Assert.assertEquals(-1, newDsn.getPort());
        checkOtherFields(newDsn);
    }

    private void checkOtherFields(Dsn newDsn) {
        Assert.assertNull(newDsn.getSecretKey());
        Assert.assertEquals("1234567813ef4c6ca4fbabc4b8f8cb7d", newDsn.getPublicKey());
        Assert.assertEquals("/", newDsn.getPath());
        Assert.assertEquals("1000001", newDsn.getProjectId());
        Assert.assertEquals("false", newDsn.getOptions().get("uncaught.handler.enabled"));
        Assert.assertEquals(" ", newDsn.getOptions().get("stacktrace.app.packages"));
    }
}
