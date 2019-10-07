package ru.kontur.vostok.hercules.http.path;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Gregory Koshelev
 */
public class PathTest {
    @Test
    public void shouldParseRootPathAsEmpty() {
        Path path = Path.of("/");
        Assert.assertEquals("/", path.getPath());
        Assert.assertEquals(0, path.getNormalizedPath().length);
    }

    @Test
    public void shouldNormalizePath() {
        Path path = Path.of("/api/handler/method/");
        Assert.assertArrayEquals(new String[]{"api", "handler", "method"}, path.getNormalizedPath());
    }
}
