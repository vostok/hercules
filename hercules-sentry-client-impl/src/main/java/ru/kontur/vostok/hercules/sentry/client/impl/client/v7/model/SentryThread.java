package ru.kontur.vostok.hercules.sentry.client.impl.client.v7.model;

import java.util.Map;

@SuppressWarnings("unused")
@Unsupported
public class SentryThread {

    private Long id;
    private Integer priority;
    private String name;
    private String state;
    private Boolean crashed;
    private Boolean current;
    private Boolean daemon;
    private SentryStackTrace stacktrace;
    private Map<String, Object> unknown;
}
