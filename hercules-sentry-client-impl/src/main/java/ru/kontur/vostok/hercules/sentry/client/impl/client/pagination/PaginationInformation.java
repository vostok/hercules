package ru.kontur.vostok.hercules.sentry.client.impl.client.pagination;

import java.net.URI;

/**
 * @author Aleksandr Yuferov
 */
public class PaginationInformation {
    private final URI uri;
    private final Relation relation;
    private final String cursor;
    private final boolean results;

    public enum Relation {
        NEXT,
        PREVIOUS,
    }

    public PaginationInformation(URI uri, Relation relation, String cursor, boolean results) {
        this.uri = uri;
        this.relation = relation;
        this.cursor = cursor;
        this.results = results;
    }

    public static PaginationInformation parse(String string) {
        String[] parts = string.split(";");

        int uriBegin = parts[0].indexOf('<') + 1;
        int uriEnd = parts[0].lastIndexOf('>');
        URI uri = URI.create(string.substring(uriBegin, uriEnd));

        Relation relation = null;
        String cursor = null;
        Boolean results = null;

        for (int partIndex = 1; partIndex < parts.length; partIndex++) {
            String[] pair = parts[partIndex].split("=");
            String key = pair[0].trim();
            String value = pair[1]
                    .substring(pair[1].indexOf('"') + 1, pair[1].lastIndexOf('"'));

            switch (key) {
                case "rel":
                    relation = Relation.valueOf(value.toUpperCase());
                    break;
                case "cursor":
                    cursor = value;
                    break;
                case "results":
                    results = Boolean.valueOf(value);
                    break;
            }
        }

        return new PaginationInformation(uri, relation, cursor, Boolean.TRUE.equals(results));
    }

    public static PaginationInformation firstPage(URI uri) {
        return new PaginationInformation(uri, Relation.NEXT, "", true);
    }

    public URI getUri() {
        return uri;
    }

    public Relation getRelation() {
        return relation;
    }

    public String getCursor() {
        return cursor;
    }

    public boolean isResults() {
        return results;
    }



}
