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

    private final List<T> data;
    private final String pagingState;

    public PagedResult(@NotNull List<T> data, @Nullable String pagingState) {
        this.data = data;
        this.pagingState = pagingState;
    }

    public List<T> getData() {
        return data;
    }

    public String getPagingState() {
        return pagingState;
    }
}
