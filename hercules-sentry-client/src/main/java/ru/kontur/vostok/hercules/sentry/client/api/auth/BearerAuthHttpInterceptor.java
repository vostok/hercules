package ru.kontur.vostok.hercules.sentry.client.api.auth;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.protocol.HttpContext;

/**
 * The interceptor is used when authentication is performed by a token.
 *
 * @author Kirill Sulim
 */
public class BearerAuthHttpInterceptor implements HttpRequestInterceptor {

    private final String token;

    public BearerAuthHttpInterceptor(String token) {
        this.token = token;
    }

    @Override
    public void process(HttpRequest httpRequest, HttpContext httpContext) {
        httpRequest.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }
}
