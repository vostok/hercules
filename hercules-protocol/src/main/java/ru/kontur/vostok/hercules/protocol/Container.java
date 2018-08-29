package ru.kontur.vostok.hercules.protocol;

import java.util.Iterator;
import java.util.Map;

public class Container implements Iterable<Map.Entry<String, Variant>> {

    private final Map<String, Variant> tags;

    public Container(Map<String, Variant> tags) {
        this.tags = tags;
    }

    public Variant get(String tagName) {
        return tags.get(tagName);
    }

    public int size() {
        return tags.size();
    }

    @Override
    public Iterator<Map.Entry<String, Variant>> iterator() {
        return tags.entrySet().iterator();
    }
}
