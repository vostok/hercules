package ru.kontur.vostok.hercules.meta.auth.validation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import ru.kontur.vostok.hercules.meta.filter.Filter;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

/**
 * @author Gregory Koshelev
 */
public class ValidationSerializer {
    private final ObjectMapper mapper = new ObjectMapper();

    public String serialize(Validation validation) {
        try {
            return validation.getApiKey() + '.' + validation.getStream() + "." + URLEncoder.encode(mapper.writeValueAsString(validation.getFilters()), "UTF-8");
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Cannot serialize filters", e);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("UTF-8 not found", e);
        }
    }

    public Validation deserialize(String value) {
        int streamOffset = value.indexOf('.');
        if (streamOffset == -1 || streamOffset == value.length()) {
            throw new IllegalArgumentException("No stream name found in string value");
        }
        String apiKey = value.substring(0, streamOffset);

        int filtersOffset = value.indexOf('.', streamOffset + 1);
        if (filtersOffset == -1 || filtersOffset == value.length()) {
            throw new IllegalArgumentException("No filters found in string value");
        }
        String stream = value.substring(streamOffset + 1, filtersOffset);
        Filter[] filters;
        try {
            filters = mapper.readValue(URLDecoder.decode(value.substring(filtersOffset + 1), "UTF-8"), Filter[].class);
        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot deserialize filters");
        }
        return new Validation(apiKey, stream, filters);
    }
}
