package ru.kontur.vostok.hercules.http.jetty;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import ru.kontur.vostok.hercules.http.handler.AsyncHttpHandler;
import ru.kontur.vostok.hercules.http.AsyncHttpServer;

import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

/**
 * @author Gregory Koshelev
 */
public class JettyServer extends AsyncHttpServer {
    private final Server server;

    public JettyServer(String hostname, int port) {
        super(hostname, port);

        server = new Server(new InetSocketAddress(hostname, port));
    }

    @Override
    protected void startInternal() {
        try {
            server.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected boolean stopInternal(long timeout, TimeUnit unit) {
        try {
            server.stop();//TODO: stop can block current thread forever, thus move stopiing to another thread with wait/notify.
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return server.isStopped();
    }

    @Override
    protected void setHandlerInternal(AsyncHttpHandler handler) {
        server.setHandler(new AbstractHandler() {
            @Override
            public void handle(
                    String target,
                    Request baseRequest,
                    HttpServletRequest request,
                    HttpServletResponse response
            ) throws IOException, ServletException {
                baseRequest.setHandled(true);
                AsyncContext asyncContext = request.startAsync();

                handler.handleAsync(new JettyHttpServerRequest(asyncContext));
            }
        });

    }
}
