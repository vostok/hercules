package ru.kontur.vostok.hercules.protocol.hpath;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Gregory Koshelev
 */
public class HPathTest {
    @Test
    public void builderTest() {
        HPath hPath = HPath.fromPath("root/next/last");

        Assert.assertEquals("root", hPath.getRootTag());

        Assert.assertEquals("root/next/last", HPath.fromTags("root", "next", "last").getPath());
    }

    @Test
    public void iteratorTest() {
        HPath hPath = HPath.fromPath("root/next/last");
        HPath.TagIterator iterator = hPath.it();

        Assert.assertTrue(iterator.hasNext());
        Assert.assertEquals("root", iterator.next());
        Assert.assertTrue(iterator.hasNext());
        Assert.assertEquals("next", iterator.next());
        Assert.assertTrue(iterator.hasNext());
        Assert.assertEquals("last", iterator.next());
        Assert.assertFalse(iterator.hasNext());
    }
}
