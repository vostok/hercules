package ru.kontur.vostok.hercules.util;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Lazy
 *
 * @author Kirill Sulim
 */
public class Lazy<T> implements Supplier<T> {

    private final Supplier<T> supplier;
    private volatile T object = null;

    public Lazy(Supplier<T> supplier) {
        this.supplier = supplier;
    }

    @Override
    public T get() {
        if (Objects.isNull(object)) {
            synchronized (this) {
                if (Objects.isNull(object)) {
                    object = supplier.get();
                }
            }
        }
        return object;
    }
}
