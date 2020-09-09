package ru.kontur.vostok.hercules.elastic.adapter.bulk;

import ru.kontur.vostok.hercules.elastic.adapter.bulk.action.IndexAction;
import ru.kontur.vostok.hercules.json.Document;

/**
 * Index request consists of index action and document.
 *
 * @author Gregory Koshelev
 */
public class IndexRequest {
    private final IndexAction action;
    private final Document document;

    public IndexRequest(IndexAction action, Document document) {
        this.action = action;
        this.document = document;
    }

    public IndexAction getAction() {
        return action;
    }

    public Document getDocument() {
        return document;
    }
}
