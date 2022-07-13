package ru.kontur.vostok.hercules.management.api.routing.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.http.HttpServerRequest;
import ru.kontur.vostok.hercules.http.HttpStatusCodes;
import ru.kontur.vostok.hercules.http.MimeTypes;
import ru.kontur.vostok.hercules.http.handler.HttpHandler;
import ru.kontur.vostok.hercules.routing.config.zk.ZookeeperReadRepository;
import ru.kontur.vostok.hercules.routing.engine.EngineConfig;

/**
 * Handler of HTTP-requests to read routing engine config from ZooKeeper.
 *
 * @author Aleksandr Yuferov
 */
public class ReadEngineConfigHandler implements HttpHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReadEngineConfigHandler.class);
    private final ZookeeperReadRepository repository;
    private final EngineConfig defaultConfig;
    private final ObjectMapper objectMapper;

    public ReadEngineConfigHandler(
            ZookeeperReadRepository repository,
            EngineConfig defaultConfig,
            ObjectMapper objectMapper
    ) {
        this.repository = repository;
        this.defaultConfig = defaultConfig;
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(HttpServerRequest request) {
        try {
            EngineConfig engineConfig = repository.fetchEngineConfig(null);
            if (engineConfig == null) {
                engineConfig = defaultConfig;
            }
            String data = objectMapper.writeValueAsString(engineConfig);
            request.complete(HttpStatusCodes.OK, MimeTypes.APPLICATION_JSON, data);
        } catch (Exception exception) {
            LOGGER.error("an error occurred while handle read config request", exception);
            request.complete(HttpStatusCodes.INTERNAL_SERVER_ERROR);
        }
    }
}
