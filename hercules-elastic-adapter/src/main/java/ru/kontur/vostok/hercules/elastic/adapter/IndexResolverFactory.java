package ru.kontur.vostok.hercules.elastic.adapter;

import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLParser;
import com.sun.org.apache.xalan.internal.xsltc.dom.SingletonIterator;
import jdk.nashorn.internal.parser.JSONParser;
import ru.kontur.vostok.hercules.elastic.adapter.IndexResolver;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;

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
