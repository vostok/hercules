package ru.kontur.vostok.hercules.elastic.adapter.bulk.action;

/**
 * Elasticsearch any action on documents.
 * <p>
 * Supports only index action.
 *
 * @author Gregory Koshelev
 */
public class AnyAction {
    private IndexAction index;

    public void setIndex(IndexAction index) {
        this.index = index;
    }

    public IndexAction asIndexAction() {
        return index;
    }

    public static boolean isIndexAction(AnyAction action) {
        return action != null && action.index != null;
    }
}
