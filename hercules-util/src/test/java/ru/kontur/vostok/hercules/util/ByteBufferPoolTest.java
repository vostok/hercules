package ru.kontur.vostok.hercules.util;

import org.junit.Assert;
import org.junit.Test;

import java.nio.ByteBuffer;

/**
 * @author Gregory Koshelev
 */
public class ByteBufferPoolTest {
    @Test
    public void reusableBufferTest() {
        ByteBuffer kilobyte = ByteBufferPool.acquire(1024);

        Assert.assertEquals(1024, kilobyte.remaining());
        Assert.assertEquals(0, kilobyte.position());
        Assert.assertEquals(1024, kilobyte.limit());

        kilobyte.putLong(42L);

        ByteBufferPool.release(kilobyte);

        int count = ByteBufferPool.count();
        long totalCapacity = ByteBufferPool.totalCapacity();

        ByteBuffer tiny = ByteBufferPool.acquire(16);

        Assert.assertEquals(16, tiny.remaining());
        Assert.assertEquals(0, tiny.position());
        Assert.assertEquals(16, tiny.limit());

        ByteBufferPool.release(tiny);

        Assert.assertEquals(count, ByteBufferPool.count());
        Assert.assertEquals(totalCapacity, ByteBufferPool.totalCapacity());
    }
}
