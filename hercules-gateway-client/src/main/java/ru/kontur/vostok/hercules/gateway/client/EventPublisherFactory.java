package ru.kontur.vostok.hercules.gateway.client;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.ThreadFactory;

/**
 * @author Daniil Zhenikhov
 */
public class EventPublisherFactory {
    private static final String RESOURCE = "gateway-client.properties";
    private static final Properties PROPERTIES = new Properties();
    private static final EventPublisherConfigurationFactory CONFIGURATION_FACTORY
            = new EventPublisherConfigurationFactory();
    private static final EventPublisherConfiguration PROPERTIES_CONFIGURATION;

    static {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        try (InputStream stream = loader.getResourceAsStream(RESOURCE)) {
            if (stream != null) {
                PROPERTIES.load(stream);

                EventPublisherConfigurationFactory.checkRequiredFields(PROPERTIES);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        PROPERTIES_CONFIGURATION = PROPERTIES.isEmpty() ? null : CONFIGURATION_FACTORY.create(PROPERTIES);
    }

    public EventPublisher create(String stream,
                                 boolean loseOnOverflow,
                                 ThreadFactory threadFactory,
                                 EventPublisherConfiguration configuration) {
        return new EventPublisher(stream,
                configuration.getPeriodMillis(),
                configuration.getBatchSize(),
                configuration.getThreads(),
                configuration.getCapacity(),
                loseOnOverflow,
                threadFactory,
                configuration.getUrl(),
                configuration.getApiKey()
        );
    }

    public EventPublisher create(String stream,
                                 boolean loseOnOverflow,
                                 ThreadFactory threadFactory) {
        if (PROPERTIES.isEmpty()) {
            throw new IllegalStateException("Can not create EventPublisher. Properties file is not set.");
        }

        return create(stream,
                loseOnOverflow,
                threadFactory,
                PROPERTIES_CONFIGURATION);
    }

    public EventPublisher create(String stream,
                                 boolean loseOnOverFlow,
                                 ThreadFactory threadFactory,
                                 String url,
                                 String apiKey) {
        return create(stream,
                loseOnOverFlow,
                threadFactory,
                CONFIGURATION_FACTORY.create(url, apiKey));
    }
}
