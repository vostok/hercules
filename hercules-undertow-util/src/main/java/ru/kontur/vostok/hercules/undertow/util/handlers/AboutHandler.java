package ru.kontur.vostok.hercules.undertow.util.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.util.application.ApplicationContext;
import ru.kontur.vostok.hercules.util.application.ApplicationContextHolder;
import ru.kontur.vostok.hercules.util.properties.PropertyDescription;
import ru.kontur.vostok.hercules.util.properties.PropertyDescriptions;
import ru.kontur.vostok.hercules.util.throwable.ThrowableUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * AboutHandler
 *
 * @author Kirill Sulim
 */
public class AboutHandler implements HttpHandler {

    private static class Props {
        static final PropertyDescription<String> COMMIT_HASH = PropertyDescriptions
                .stringProperty("git.commit.id")
                .withDefaultValue("unknown")
                .build();

        static final PropertyDescription<String> VERSION = PropertyDescriptions
                .stringProperty("git.build.version")
                .withDefaultValue("unknown")
                .build();
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(AboutHandler.class);

    private static final String GIT_PROPERTIES = "git.properties";

    private final String aboutJsonString;

    public AboutHandler() {
        ApplicationContext applicationContext = ApplicationContextHolder.get();


        Properties properties = new Properties();
        try {
            InputStream resourceAsStream = getClass().getClassLoader().getResourceAsStream(GIT_PROPERTIES);
            properties.load(resourceAsStream);
        }
        catch (IOException e) {
            LOGGER.warn("Cannot load '{}' file", GIT_PROPERTIES, e);
        }
        final String commitHash = Props.COMMIT_HASH.extract(properties);
        final String version = Props.VERSION.extract(properties);

        AboutInfo info = new AboutInfo(
                applicationContext.getName(),
                version,
                commitHash,
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
