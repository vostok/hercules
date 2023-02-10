package ru.kontur.vostok.hercules.elastic.sink;

import ru.kontur.vostok.hercules.json.Document;

import java.util.Objects;

/**
 * @author Gregory Koshelev
 */
public class ElasticDocument {
    private final String id;
    private final String index;
    private final Document document;

    public ElasticDocument(String id, String index, Document document) {
        this.id = id;
        this.index = index;
        this.document = document;
    }

    public String id() {
        return id;
    }

    public String index() {
        return index;
    }

    public Document document() {
        return document;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ElasticDocument that = (ElasticDocument) o;

        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
