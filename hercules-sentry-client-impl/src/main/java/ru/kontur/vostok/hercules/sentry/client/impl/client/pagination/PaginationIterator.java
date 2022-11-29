package ru.kontur.vostok.hercules.sentry.client.impl.client.pagination;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.Objects;
import ru.kontur.vostok.hercules.sentry.client.impl.client.pagination.PaginationInformation.Relation;

/**
 * @author Aleksandr Yuferov
 */
public class PaginationIterator<T> {

    private final RequestPerformer<T> requestPerformer;
    private PaginationInformation nextPageInfo;
    private HttpResponse<T> currentResponse = null;

    public PaginationIterator(URI baseUri, RequestPerformer<T> requestPerformer) {
        this.requestPerformer = requestPerformer;
        this.nextPageInfo = PaginationInformation.firstPage(baseUri);
    }

    public boolean hasNext() throws IOException, InterruptedException {
        if (currentResponse != null) {
            return true;
        }
        if (nextPageInfo == null) {
            return false;
        }

        currentResponse = requestPerformer.performRequest(nextPageInfo.getUri());
        nextPageInfo = extractNextPageInformation();

        return true;
    }

    public HttpResponse<T> next() {
        var current = this.currentResponse;
        this.currentResponse = null;
        return current;
    }

    private PaginationInformation extractNextPageInformation() {
        return currentResponse.headers().firstValue("link").stream()
                .flatMap(value -> Arrays.stream(value.split(",")))
                .map(string -> {
                    try {
                        return PaginationInformation.parse(string);
                    } catch (Exception exception) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .filter(pi -> pi.getRelation() == Relation.NEXT && pi.isResults())
                .findFirst()
                .orElse(null);
    }

    @FunctionalInterface
    public interface RequestPerformer<T> {

        HttpResponse<T> performRequest(URI uri) throws IOException, InterruptedException;
    }
}
