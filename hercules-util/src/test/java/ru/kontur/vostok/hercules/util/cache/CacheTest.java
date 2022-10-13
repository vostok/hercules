package ru.kontur.vostok.hercules.util.cache;

import org.junit.Test;
import ru.kontur.vostok.hercules.util.functional.Result;

import java.util.function.Supplier;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CacheTest {

    @Test
    public void shouldCreateNewCachedAfterError() throws InterruptedException {
        Cache<String, String> cache = new Cache<>(500);

        StringSupplier supplier = mock(StringSupplier.class);
        when(supplier.get())
                .thenReturn(Result.ok("value"))
                .thenReturn(Result.error("error"));

        assertEquals("value", cache.cacheAndGet("test", key -> supplier.get()).get());

        Thread.sleep(1000);

        assertEquals("value", cache.cacheAndGet("test", key -> supplier.get()).get());
        assertEquals("value", cache.cacheAndGet("test", key -> supplier.get()).get());

        verify(supplier, times(2)).get();
    }

    interface StringSupplier extends Supplier<Result<String, String>> {
    }
}