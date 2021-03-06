package ru.kontur.vostok.hercules.http.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.application.Application;
import ru.kontur.vostok.hercules.http.HttpServerRequest;
import ru.kontur.vostok.hercules.http.HttpStatusCodes;
import ru.kontur.vostok.hercules.http.MimeTypes;

/**
 * @author Gregory Koshelev
 */
public class AboutHandler implements HttpHandler {
    private static Logger LOGGER = LoggerFactory.getLogger(AboutHandler.class);

    private volatile String about;

    @Override
    public void handle(HttpServerRequest request) {
        if (about == null) {
            try {
                about = new ObjectMapper().writeValueAsString(Application.context());
            } catch (JsonProcessingException e) {
                LOGGER.error("Cannot construct about message due to serialization exception", e);
                about = "{}";
            }
        }

        request.complete(HttpStatusCodes.OK, MimeTypes.APPLICATION_JSON, about);
    }
}
