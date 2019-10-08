package ru.kontur.vostok.hercules.http.handler;

import org.junit.Test;
import ru.kontur.vostok.hercules.http.HttpMethod;
import ru.kontur.vostok.hercules.http.HttpServerRequest;
import ru.kontur.vostok.hercules.http.NotSupportedHttpMethodException;

import java.util.Properties;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Gregory Koshelev
 */
public class RouteHandlerTest {
    @Test
    public void shouldMatchExactPathFirst() throws NotSupportedHttpMethodException {
        HttpHandler aHandler = mock(HttpHandler.class);
        HttpHandler bHandler = mock(HttpHandler.class);
        HttpHandler cHandler = mock(HttpHandler.class);
        HttpHandler exactHandler = mock(HttpHandler.class);

        RouteHandlerBuilder builder = new RouteHandlerBuilder(new Properties());
        builder.get("/:a/bb/c", aHandler);
        builder.get("/aaa/:b/c", bHandler);
        builder.get("/aaa/bb/:c", cHandler);
        builder.get("/aaa/bb/c", exactHandler);
        RouteHandler routeHandler = builder.build();

        HttpServerRequest request = mock(HttpServerRequest.class);
        when(request.getMethod()).thenReturn(HttpMethod.GET);
        when(request.getPath()).thenReturn("/aaa/bb/c");

        routeHandler.handle(request);

        verify(exactHandler).handle(request);
        verify(aHandler, never()).handle(any());
        verify(bHandler, never()).handle(any());
        verify(cHandler, never()).handle(any());
    }

    @Test
    public void shouldPreserveOrderWhenMatch() throws NotSupportedHttpMethodException {
        HttpHandler aHandler = mock(HttpHandler.class);
        HttpHandler bHandler = mock(HttpHandler.class);
        HttpHandler cHandler = mock(HttpHandler.class);

        RouteHandlerBuilder builder = new RouteHandlerBuilder(new Properties());
        builder.get("/aaa/:b/c", bHandler);
        builder.get("/:a/bb/c", aHandler);
        builder.get("/aaa/bb/:c", cHandler);
        RouteHandler routeHandler = builder.build();

        HttpServerRequest request = mock(HttpServerRequest.class);
        when(request.getMethod()).thenReturn(HttpMethod.GET);
        when(request.getPath()).thenReturn("/aaa/bb/c");

        routeHandler.handle(request);

        verify(bHandler).handle(any());
        verify(aHandler, never()).handle(any());
        verify(cHandler, never()).handle(any());
    }
}
