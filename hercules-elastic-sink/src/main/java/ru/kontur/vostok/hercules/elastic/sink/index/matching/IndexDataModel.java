package ru.kontur.vostok.hercules.elastic.sink.index.matching;

import java.util.List;
import java.util.Map;

/**
 * Raw model for index data. The model is parsed from JSON
 *
 * @author Petr Demenev
 */
public class IndexDataModel {
    private String index;
    private List<Map<String, String>> tagMaps;

    public String getIndex() {
        return index;
    }

    public List<Map<String, String>> getTagMaps() {
        return tagMaps;
    }

    public void setIndex(String index) {
        this.index = index;
    }

    public void setTagMaps(List<Map<String, String>> tagMaps) {
        this.tagMaps = tagMaps;
    }
}
