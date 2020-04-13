package ru.kontur.vostok.hercules.elastic.adapter.bulk.action;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Elasticsearch index action.
 *
 * @author Gregory Koshelev
 */
public class IndexAction {
    private String index;
    private String type;
    private String id;

    public String getIndex() {
        return index;
    }

    public String getType() {
        return type;
    }

    public String getId() {
        return id;
    }

    @JsonProperty("_index")
    public void setIndex(String index) {
        this.index = index;
    }

    @JsonProperty("_type")
    public void setType(String type) {
        this.type = type;
    }

    @JsonProperty("_id")
    public void setId(String id) {
        this.id = id;
    }
}
