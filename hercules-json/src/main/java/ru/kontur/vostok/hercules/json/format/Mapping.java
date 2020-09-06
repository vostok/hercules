package ru.kontur.vostok.hercules.json.format;

import ru.kontur.vostok.hercules.protocol.hpath.HTree;

import java.util.Iterator;
import java.util.List;

/**
 * Data-class holds configured mappers.
 *
 * @author Gregory Koshelev
 * @see MappingLoader
 */
public class Mapping {
    private final List<Mapper> mappers;
    private final HTree<Boolean> mappableTags;

    public Mapping(List<Mapper> mappers, HTree<Boolean> mappableTags) {
        this.mappers = mappers;
        this.mappableTags = mappableTags;
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
     * Create navigator over {@link HTree} of mappable tags.
     * <p>
     * Tag is mappable if it has defined mappers.
     * <p>
     * Navigator helps {@link EventToJsonFormatter} to determine which tags don't have defined mapping.
     *
     * @return navigator
     */
    public HTree<Boolean>.Navigator navigator() {
        return mappableTags.navigator();
    }
}
