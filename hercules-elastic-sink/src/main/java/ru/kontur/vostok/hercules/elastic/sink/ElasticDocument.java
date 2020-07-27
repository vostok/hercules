package ru.kontur.vostok.hercules.elastic.sink;

import ru.kontur.vostok.hercules.json.Document;

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
}
