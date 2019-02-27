package ru.kontur.hercules.tracing.api.cassandra;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * PagedResult
 *
 * @author Kirill Sulim
 */
public class PagedResult<T> {

    private final List<T> result;
    private final String pagingState;

    public PagedResult(@NotNull List<T> result, @Nullable String pagingState) {
        this.result = result;
        this.pagingState = pagingState;
    }

    public List<T> getResult() {
        return result;
    }

    public String getPagingState() {
        return pagingState;
    }
}
