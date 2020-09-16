package ru.kontur.vostok.hercules.tracing.api;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Page has the list of elements and the state of it.
 *
 * @author Gregory Koshelev
 */
public class Page<T> {
    private final List<T> elements;
    private final String state;

    public Page(@NotNull List<T> elements, @Nullable String pagingState) {
        this.elements = elements;
        this.state = pagingState;
    }

    /**
     * Elements. If page if empty, then return empty list.
     *
     * @return the list of elements
     */
    public List<T> elements() {
        return elements;
    }

    /**
     * The page state. If the state is undefined, then return {@code null}.
     *
     * @return state
     */
    public String state() {
        return state;
    }
}
