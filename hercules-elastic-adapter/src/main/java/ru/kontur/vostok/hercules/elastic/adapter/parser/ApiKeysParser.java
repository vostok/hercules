package ru.kontur.vostok.hercules.elastic.adapter.parser;

import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLParser;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * @author Daniil Zhenikhov
 */
public class ApiKeysParser {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper(new YAMLFactory());

    public static Map<String, List<String>> parse(InputStream inputStream) throws IOException {
        ApiKeys apiKeys = OBJECT_MAPPER.readValue(inputStream, ApiKeys.class);
        return apiKeys.getValues();
    }

    private ApiKeysParser() {
    }
}
