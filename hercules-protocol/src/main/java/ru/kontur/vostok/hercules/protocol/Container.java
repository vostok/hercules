package ru.kontur.vostok.hercules.protocol;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

public class Container implements Iterable<Map.Entry<String, Variant>> {

    private final Map<String, Variant> fields;

    public Container(Map<String, Variant> fields) {
        this.fields = fields;
    }

    public Variant get(String fieldName) {
        return fields.get(fieldName);
    }

    public int size() {
        return fields.size();
    }

    @Override
    public Iterator<Map.Entry<String, Variant>> iterator() {
        return fields.entrySet().iterator();
    }
}
