package ru.kontur.vostok.hercules.elastic.adapter;

import ru.kontur.vostok.hercules.elastic.adapter.parser.ApiKeysParser;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.*;

/**
 * @author Daniil Zhenikhov
 */
public class IndexResolverFactory {
    private static final String DEFAULT_RESOURCE_NAME = "api-keys.yml";

    private final static IndexResolver INSTANCE;

    static {
        String filename = System.getProperty("api.keys.location");
        boolean fromResources = false;
        if (Objects.isNull(filename)) {
            filename = DEFAULT_RESOURCE_NAME;
            fromResources = true;
        }
        try {
            InputStream inputStream = readApiKeys(filename, fromResources);
            INSTANCE = new IndexResolver(ApiKeysParser.parse(inputStream));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public static IndexResolver getInstance() {
        return INSTANCE;
    }

    private static InputStream readApiKeys(String filename, boolean fromResources) throws FileNotFoundException {
        if (fromResources) {
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            return loader.getResourceAsStream(filename);
        }
        return new FileInputStream(filename);

    }

    private IndexResolverFactory() {

    }
}
