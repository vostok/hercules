package ru.kontur.vostok.hercules.protocol;

import java.util.Iterator;
import java.util.Map;

public class Container implements Iterable<Map.Entry<String, Variant>> {

    private final Map<String, Variant> variants;

    public Container(Map<String, Variant> variants) {
        this.variants = variants;
    }

    public Variant get(String fieldName) {
        return variants.get(fieldName);
    }

    public int size() {
        return variants.size();
    }

    @Override
    public Iterator<Map.Entry<String, Variant>> iterator() {
        return variants.entrySet().iterator();
    }
}
