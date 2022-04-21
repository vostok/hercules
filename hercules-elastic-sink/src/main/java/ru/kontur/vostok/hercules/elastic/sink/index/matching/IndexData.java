package ru.kontur.vostok.hercules.elastic.sink.index.matching;

import java.util.List;

/**
 * Index data with index name and corresponding list of tag maps
 *
 * @author Petr Demenev
 */
public class IndexData {
    private final String index;
    private final List<TagMap> tagMaps;

    public IndexData(String index, List<TagMap> tagMaps) {
        this.index = index;
        this.tagMaps = tagMaps;
    }

    public String getIndex() {
        return index;
    }

    public List<TagMap> getTagMaps() {
        return tagMaps;
    }
}
