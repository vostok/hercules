package ru.kontur.vostok.hercules.sentry.client.impl.client.v7.model;

import java.util.Map;

@Unsupported
@SuppressWarnings("unused")
public class Request {

    private String url;
    private String method;
    private String queryString;
    private Object data;
    private String cookies;
    private Map<String, String> headers;
    private Map<String, String> env;
    private Map<String, String> other;
    private Map<String, Object> unknown;
}
