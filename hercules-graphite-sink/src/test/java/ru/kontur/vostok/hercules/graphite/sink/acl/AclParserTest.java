package ru.kontur.vostok.hercules.graphite.sink.acl;

import org.junit.Assert;
import org.junit.Test;
import ru.kontur.vostok.hercules.util.properties.ConfigsUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.List;

/**
 * @author Vladimir Tsypaev
 */
public class AclParserTest {

    @Test
    public void shouldParseAcl() throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                ConfigsUtil.readConfig("metric.property", "metrics.acl")));
        List<AccessControlEntry> acl = AclParser.parse(reader);

        Assert.assertEquals(3, acl.size());

        Assert.assertFalse(acl.get(0).isPermit());
        Assert.assertEquals(4, acl.get(0).getPattern().length);

        Assert.assertTrue(acl.get(1).isPermit());
        Assert.assertEquals(3, acl.get(1).getPattern().length);

        Assert.assertFalse(acl.get(2).isPermit());
        Assert.assertEquals(2, acl.get(2).getPattern().length);
    }

    @Test
    public void shouldReturnEmptyList() throws IOException {
        BufferedReader reader = prepareReader("");

        List<AccessControlEntry> list = AclParser.parse(reader);

        Assert.assertTrue(list.isEmpty());
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionIfIncorrectFileContent() throws IOException {
        String source = "DENY:test.foo.bar.*";
        BufferedReader reader = prepareReader(source);

        AclParser.parse(reader);
    }

    @Test
    public void shouldSkipEmptyLines() throws IOException {
        String source = "\n\n\nDENY test.foo.bar.*\n\n\n";
        BufferedReader reader = prepareReader(source);

        List<AccessControlEntry> result = AclParser.parse(reader);

        Assert.assertEquals(1, result.size());
    }

    @Test
    public void shouldSupportLineComments() throws IOException {
        String source = "# some comment here\n" +
                "DENY test.foo.bar.*\n" +
                "# some comment here too";

        BufferedReader reader = prepareReader(source);
        List<AccessControlEntry> result = AclParser.parse(reader);

        Assert.assertEquals(1, result.size());
    }

    private static BufferedReader prepareReader(String data) {
        return new BufferedReader(new StringReader(data));
    }
}
