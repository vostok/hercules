package ru.kontur.vostok.hercules.elastic.adapter.bulk;

import ru.kontur.vostok.hercules.elastic.adapter.bulk.action.IndexAction;

import java.util.Map;

/**
 * Index request consists of index action and document.
 *
 * @author Gregory Koshelev
 */
public class IndexRequest {
    private final IndexAction action;
    private final Map<String, Object> document;

    public IndexRequest(IndexAction action, Map<String, Object> document) {
        this.action = action;
        this.document = document;
    }

    public IndexAction getAction() {
        return action;
    }

    public Map<String, Object> getDocument() {
        return document;
    }
}
