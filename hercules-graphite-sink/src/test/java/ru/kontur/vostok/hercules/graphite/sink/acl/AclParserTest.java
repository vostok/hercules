package ru.kontur.vostok.hercules.graphite.sink.acl;

import org.junit.Assert;
import org.junit.Test;
import ru.kontur.vostok.hercules.util.properties.ConfigsUtil;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * @author Vladimir Tsypaev
 */
public class AclParserTest {

    @Test
    public void shouldParseAcl() {
        InputStream in = ConfigsUtil.readConfig("metric.property", "metrics.acl");
        List<AccessControlEntry> acl = AclParser.parse(in);

        Assert.assertEquals(3, acl.size());

        Assert.assertFalse(acl.get(0).isPermit());
        Assert.assertEquals(4, acl.get(0).getPattern().length);

        Assert.assertTrue(acl.get(1).isPermit());
        Assert.assertEquals(3, acl.get(1).getPattern().length);

        Assert.assertFalse(acl.get(2).isPermit());
        Assert.assertEquals(2, acl.get(2).getPattern().length);
    }

    @Test
    public void shouldReturnEmptyList() {
        List<AccessControlEntry> list = AclParser.parse(new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8)));
        Assert.assertTrue(list.isEmpty());
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionIfIncorrectFileContent() {
        String source = "DENY:test.foo.bar.*";
        AclParser.parse(new ByteArrayInputStream(source.getBytes(StandardCharsets.UTF_8)));
    }
}
