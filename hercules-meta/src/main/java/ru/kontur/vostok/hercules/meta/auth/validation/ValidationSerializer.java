package ru.kontur.vostok.hercules.meta.auth.validation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import ru.kontur.vostok.hercules.meta.filter.Filter;
import ru.kontur.vostok.hercules.util.text.StringUtil;

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
        String[] parts;
        try {
            parts = StringUtil.split(value, '.', 3);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Cannot deserialize validation", e);
        }

        String apiKey = parts[0];

        String stream = parts[1];

        Filter[] filters;
        try {
            filters = mapper.readValue(URLDecoder.decode(parts[2], "UTF-8"), Filter[].class);
        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot deserialize filters", e);
        }

        return new Validation(apiKey, stream, filters);
    }
}
