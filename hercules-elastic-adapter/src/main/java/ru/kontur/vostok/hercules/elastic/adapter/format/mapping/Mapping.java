package ru.kontur.vostok.hercules.elastic.adapter.format.mapping;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Data-class holds configured mappers.
 *
 * @author Gregory Koshelev
 */
public class Mapping {
    private static final Mapping EMPTY = new Mapping(Collections.emptyList());

    private final List<Mapper> mappers;

    public Mapping(List<Mapper> mappers) {
        this.mappers = mappers;
    }

    /**
     * Create iterator over configured mappers.
     *
     * @return iterator
     */
    public Iterator<Mapper> iterator() {
        return mappers.iterator();
    }

    /**
     * Return empty mapping.
     *
     * @return empty mapping
     */
    public static Mapping empty() {
        return EMPTY;
    }
}
