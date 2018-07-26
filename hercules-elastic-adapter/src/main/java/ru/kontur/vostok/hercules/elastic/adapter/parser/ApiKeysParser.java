package ru.kontur.vostok.hercules.elastic.adapter;

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
    private static final String EXCEPTION_MESSAGE = "File has wrong structure";
    private static final String ROOT_KEY = "elk_api_keys";
    private static final YAMLFactory YAML_FACTORY = new YAMLFactory();

    public static Map<String, List<String>> parse(InputStream inputStream) throws IOException {
        YAMLParser parser = YAML_FACTORY.createParser(inputStream);
        Map<String, List<String>> result = new HashMap<>();
        int depth = 0;
        String currentApiKey = null;
        List<String> currentIndexesList = null;

        JsonToken token = parser.nextToken();

        while (token != null) {
            switch (parser.getCurrentToken()) {
                case START_OBJECT:
                    depth += 1;
                    if (depth > 2) {
                        throw new IllegalStateException(EXCEPTION_MESSAGE);
                    }
                    break;

                case FIELD_NAME:
                    if (depth < 2) {
                        if (!parser.getText().equals(ROOT_KEY)) {
                            throw new IllegalStateException(EXCEPTION_MESSAGE);
                        }
                        break;
                    }

                    if (Objects.nonNull(currentApiKey)) {
                        throw new IllegalStateException(EXCEPTION_MESSAGE);
                    }

                    currentApiKey = parser.getText();
                    result.put(currentApiKey, new ArrayList<>());
                    break;

                case START_ARRAY:
                    if (Objects.isNull(currentApiKey)) {
                        throw new IllegalStateException(EXCEPTION_MESSAGE);
                    }

                    currentIndexesList = result.get(currentApiKey);
                    break;

                case VALUE_STRING:
                    if (Objects.isNull(currentIndexesList)) {
                        throw new IllegalStateException(EXCEPTION_MESSAGE);
                    }

                    currentIndexesList.add(parser.getText());
                    break;

                case END_ARRAY:
                    currentApiKey = null;
                    currentIndexesList = null;
                    break;

                case END_OBJECT:
                    break;

                default:
                    throw new IllegalStateException(EXCEPTION_MESSAGE);
            }

            token = parser.nextToken();
        }
        return  result;
    }

    private ApiKeysParser() {

    }
}
