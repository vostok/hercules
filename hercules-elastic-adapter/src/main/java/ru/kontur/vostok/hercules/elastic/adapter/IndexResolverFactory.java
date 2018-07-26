package ru.kontur.vostok.hercules.elastic.adapter;

import ru.kontur.vostok.hercules.elastic.adapter.parser.ApiKeys;
import ru.kontur.vostok.hercules.elastic.adapter.parser.ApiKeysParser;
import ru.kontur.vostok.hercules.util.properties.ConfigsUtil;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * @author Daniil Zhenikhov
 */
public class IndexResolverFactory {
    private static final String DEFAULT_RESOURCE_NAME = "api-keys.yml";
    private static final String PROPERTY_NAME = "api.keys.location";

    private final static IndexResolver INSTANCE;

    static {
        InputStream inputStream = ConfigsUtil.readConfig(PROPERTY_NAME, DEFAULT_RESOURCE_NAME);
        try {
            INSTANCE = new IndexResolver(ApiKeysParser.parse(inputStream));
        } catch (IOException e) {
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
