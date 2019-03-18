package ru.kontur.vostok.hercules.sentry.sink;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import io.sentry.marshaller.json.JsonMarshaller;

import java.io.IOException;
import java.io.OutputStream;

/**
 * CustomJsonMarshaller
 *
 * @author Kirill Sulim
 */
public class CustomJsonMarshaller extends JsonMarshaller {

    private final JsonFactory jsonFactory = new JsonFactory();

    public CustomJsonMarshaller(int maxMessageLength) {
        super(maxMessageLength);
    }

    @Override
    protected JsonGenerator createJsonGenerator(OutputStream destination) throws IOException {
        return new CustomSentryJsonGenerator(jsonFactory.createGenerator(destination));
    }
}
