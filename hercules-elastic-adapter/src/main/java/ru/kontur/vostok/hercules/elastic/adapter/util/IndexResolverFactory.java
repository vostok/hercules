package ru.kontur.vostok.hercules.elastic.adapter.util;

import ru.kontur.vostok.hercules.elastic.adapter.util.IndexResolver;

import java.util.Collections;

/**
 * @author Daniil Zhenikhov
 */
public class IndexResolverFactory {
    private final static IndexResolver INSTANCE = new IndexResolver(Collections.emptyMap());

    public static IndexResolver getInstance() {
        return INSTANCE;
    }

    private IndexResolverFactory() {

    }
}
