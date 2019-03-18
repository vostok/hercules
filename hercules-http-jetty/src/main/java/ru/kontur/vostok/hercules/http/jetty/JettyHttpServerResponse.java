package ru.kontur.vostok.hercules.http.jetty;

import ru.kontur.vostok.hercules.http.HttpServerResponse;

import javax.servlet.http.HttpServletResponse;

/**
 * @author Gregory Koshelev
 */
public class JettyHttpServerResponse implements HttpServerResponse {
    private final HttpServletResponse httpServletResponse;

    JettyHttpServerResponse(HttpServletResponse httpServletResponse) {
        this.httpServletResponse = httpServletResponse;
    }

    @Override
    public void setStatusCode(int code) {
        httpServletResponse.setStatus(code);
    }

    @Override
    public void setHeader(String header, String value) {
        httpServletResponse.setHeader(header, value);
    }
}
