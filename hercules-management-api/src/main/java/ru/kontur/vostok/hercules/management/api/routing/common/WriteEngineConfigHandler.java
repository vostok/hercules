package ru.kontur.vostok.hercules.management.api.routing.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.http.HttpServerRequest;
import ru.kontur.vostok.hercules.http.HttpStatusCodes;
import ru.kontur.vostok.hercules.http.handler.HttpHandler;
import ru.kontur.vostok.hercules.routing.config.zk.ZookeeperWriteRepository;
import ru.kontur.vostok.hercules.routing.engine.EngineConfig;
import ru.kontur.vostok.hercules.routing.engine.EngineConfigDeserializer;
import ru.kontur.vostok.hercules.util.validation.ValidationResult;
import ru.kontur.vostok.hercules.util.validation.Validator;

/**
 * Handler of HTTP-requests to change routing engine config in ZooKeeper.
 *
 * @author Aleksandr Yuferov
 */
public class WriteEngineConfigHandler implements HttpHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(WriteEngineConfigHandler.class);
    private final ZookeeperWriteRepository writeRepository;
    private final Validator<EngineConfig> validator;
    private final EngineConfigDeserializer deserializer;

    public WriteEngineConfigHandler(
            ZookeeperWriteRepository writeRepository,
            Validator<EngineConfig> configValidator,
            EngineConfigDeserializer deserializer
    ) {
        this.writeRepository = writeRepository;
        this.validator = configValidator;
        this.deserializer = deserializer;
    }

    @Override
    public void handle(HttpServerRequest request) {
        request.readBodyAsync(this::handleData);
    }

    private void handleData(HttpServerRequest request, byte[] bytes) {
        try {
            EngineConfig engineConfig = deserializer.deserialize(bytes);
            ValidationResult validationResult = validator.validate(engineConfig);
            if (validationResult.isError()) {
                request.complete(HttpStatusCodes.BAD_REQUEST, "plain/text", validationResult.error());
                return;
            }
            if (writeRepository.tryStoreEngineConfig(engineConfig)) {
                request.complete(HttpStatusCodes.OK);
                return;
            }
        } catch (Exception exception) {
            LOGGER.error("an error occurred while handle request of engine config change", exception);
        }
        request.complete(HttpStatusCodes.INTERNAL_SERVER_ERROR);
    }
}
