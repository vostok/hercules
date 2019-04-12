package ru.kontur.vostok.hercules.sentry.api.auth;

import org.apache.http.HttpException;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.protocol.HttpContext;

import java.io.IOException;

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
    public void process(HttpRequest httpRequest, HttpContext httpContext) throws HttpException, IOException {
        httpRequest.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }
}
