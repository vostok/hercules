package ru.kontur.vostok.hercules.sentry.client.impl.client.v7.model;

import java.time.Instant;
import java.util.Map;
import ru.kontur.vostok.hercules.sentry.client.SentryLevel;

/**
 * @author Aleksandr Yuferov
 */
@SuppressWarnings("unused")
@Unsupported
public class Breadcrumb {

    private Instant timestamp;
    private String message;
    private String type;
    private Map<String, Object> data;
    private String category;
    private SentryLevel level;
    private Map<String, Object> unknown;
}
