package ru.kontur.vostok.hercules.sentry.sink;

import io.sentry.DefaultSentryClientFactory;
import io.sentry.marshaller.json.JsonMarshaller;

/**
 * CustomClientFactory
 *
 * @author Kirill Sulim
 */
public class CustomClientFactory extends DefaultSentryClientFactory {

    @Override
    protected JsonMarshaller createJsonMarshaller(int maxMessageLength) {
        return new CustomJsonMarshaller(maxMessageLength);
    }
}
