package ru.kontur.vostok.hercules.sentry.client.impl.v9.connector;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import io.sentry.marshaller.json.JsonMarshaller;

import java.io.IOException;
import java.io.OutputStream;

/**
 * HerculesJsonMarshaller
 *
 * @author Kirill Sulim
 */
public class HerculesJsonMarshaller extends JsonMarshaller {

    private final JsonFactory jsonFactory = new JsonFactory();

    public HerculesJsonMarshaller(int maxMessageLength) {
        super(maxMessageLength);
    }

    @Override
    protected JsonGenerator createJsonGenerator(OutputStream destination) throws IOException {
        return new HerculesSentryJsonGenerator(jsonFactory.createGenerator(destination));
    }
}
