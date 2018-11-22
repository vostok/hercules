package ru.kontur.vostok.hercules.undertow.util.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import ru.kontur.vostok.hercules.undertow.util.ResponseUtil;
import ru.kontur.vostok.hercules.util.application.ApplicationContextHolder;

import java.nio.charset.StandardCharsets;

/**
 * AboutHandler
 *
 * @author Kirill Sulim
 */
public class AboutHandler implements HttpHandler {

    public static final AboutHandler INSTANCE = new AboutHandler();

    private final ObjectMapper objectMapper;

    public AboutHandler() {
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        final String jsonString = objectMapper.writeValueAsString(ApplicationContextHolder.get());
        ResponseUtil.okJson(exchange, jsonString);
    }
}
