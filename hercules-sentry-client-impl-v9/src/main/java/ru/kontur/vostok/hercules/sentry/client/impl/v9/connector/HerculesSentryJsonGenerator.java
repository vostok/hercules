package ru.kontur.vostok.hercules.sentry.client.impl.v9.connector;

import com.fasterxml.jackson.core.JsonGenerator;
import io.sentry.marshaller.json.SentryJsonGenerator;

/**
 * HerculesSentryJsonGenerator
 *
 * @author Kirill Sulim
 */
public class HerculesSentryJsonGenerator extends SentryJsonGenerator {

    private static final int MAX_METAL_STRING_LENGTH = 200_000; // 200 for stack frame * 100 stack size * 10 exceptions

    public HerculesSentryJsonGenerator(JsonGenerator generator) {
        super(generator);

        super.setMaxLengthString(MAX_METAL_STRING_LENGTH);
    }
}
