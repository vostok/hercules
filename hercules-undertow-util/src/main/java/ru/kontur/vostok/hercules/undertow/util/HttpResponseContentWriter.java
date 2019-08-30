package ru.kontur.vostok.hercules.undertow.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.http.HttpServerRequest;
import ru.kontur.vostok.hercules.http.HttpStatusCodes;
import ru.kontur.vostok.hercules.http.MimeTypes;

/**
 * Utility class is used to write content to HTTP response.
 *
 * @author Gregory Koshelev
 */
public class HttpResponseContentWriter {
    public static final Logger LOGGER = LoggerFactory.getLogger(HttpResponseContentWriter.class);

    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * Write JSON to HttpResponse.
     * <p>
     * If object cannot be serialized to JSON, then complete request with 500 status code.
     *
     * @param object  the object to write
     * @param request the HTTP request
     */
    public static void writeJson(@NotNull Object object, @NotNull HttpServerRequest request) {
        String responseContent;
        try {
            responseContent = mapper.writeValueAsString(object);
        } catch (JsonProcessingException ex) {
            LOGGER.error("Cannot serialize object of type " + object.getClass(), ex);
            request.complete(HttpStatusCodes.INTERNAL_SERVER_ERROR);
            return;
        }

        request.getResponse().setContentType(MimeTypes.APPLICATION_JSON);
        request.getResponse().send(responseContent);
    }

    private HttpResponseContentWriter() {
        /* static class */
    }
}
