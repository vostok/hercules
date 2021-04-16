package ru.kontur.vostok.hercules.elastic.sink.error;


/**
 * ElasticError is the POJO representation of indexing error in Elastic.
 *
 * @author Gregory Koshelev
 */
public class ElasticError {
    private final ErrorGroup group;
    private final String type;
    private final String index;
    private final String documentId;
    private final String details;

    public ElasticError(ErrorGroup group, String type, String index, String documentId, String details) {
        this.group = group;
        this.type = type;
        this.index = index;
        this.documentId = documentId;
        this.details = details;
    }

    public ErrorGroup group() {
        return group;
    }

    public String type() {
        return type;
    }

    public String index() {
        return index;
    }

    public String documentId() {
        return documentId;
    }

    public String details() {
        return details;
    }
}
