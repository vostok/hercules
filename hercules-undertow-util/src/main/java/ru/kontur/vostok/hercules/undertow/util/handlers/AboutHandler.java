package ru.kontur.vostok.hercules.undertow.util.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import ru.kontur.vostok.hercules.util.application.ApplicationContext;
import ru.kontur.vostok.hercules.util.application.ApplicationContextHolder;
import ru.kontur.vostok.hercules.util.throwable.ThrowableUtil;

import java.nio.charset.StandardCharsets;

/**
 * AboutHandler
 *
 * @author Kirill Sulim
 */
public class AboutHandler implements HttpHandler {

    private final String aboutJsonString;

    public AboutHandler() {
        ApplicationContext applicationContext = ApplicationContextHolder.get();

        AboutInfo info = new AboutInfo(
                applicationContext.getName(),
                "",
                "",
                applicationContext.getEnvironment(),
                applicationContext.getInstanceId()
        );

        ObjectMapper objectMapper = new ObjectMapper();
        aboutJsonString = ThrowableUtil.toUnchecked(() -> objectMapper.writeValueAsString(info));
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        exchange.setStatusCode(200);
        exchange.getResponseSender().send(aboutJsonString, StandardCharsets.UTF_8);
        exchange.endExchange();
    }
}
