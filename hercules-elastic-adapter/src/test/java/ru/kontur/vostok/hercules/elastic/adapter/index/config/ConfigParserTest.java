package ru.kontur.vostok.hercules.elastic.adapter.index.config;

import org.junit.Assert;
import org.junit.Test;
import ru.kontur.vostok.hercules.elastic.adapter.index.IndexMeta;
import ru.kontur.vostok.hercules.protocol.TinyString;
import ru.kontur.vostok.hercules.protocol.Variant;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * @author Gregory Koshelev
 */
public class ConfigParserTest {
    @Test
    public void shouldParseIndexMeta() {
        String source = "{\"index_my_production\": {\"stream\": \"stream_project\", \"properties\": {\"project\": \"my-project\", \"environment\": \"production\"}}}";
        Map<String, IndexMeta> result = ConfigParser.parse(new ByteArrayInputStream(source.getBytes(StandardCharsets.UTF_8)));

        Assert.assertEquals(1, result.size());
        Assert.assertTrue(result.containsKey("index_my_production"));

        IndexMeta meta = result.get("index_my_production");
        Assert.assertEquals("stream_project", meta.getStream());

        Map<TinyString, Variant> properties = meta.getProperties();
        Assert.assertEquals(2, properties.size());
        Assert.assertEquals(Variant.ofString("my-project").toString(), properties.get(TinyString.of("project")).toString());
        Assert.assertEquals(Variant.ofString("production").toString(), properties.get(TinyString.of("environment")).toString());
    }
}
