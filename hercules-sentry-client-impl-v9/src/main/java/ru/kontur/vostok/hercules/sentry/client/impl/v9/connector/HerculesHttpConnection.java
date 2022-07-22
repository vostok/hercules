package ru.kontur.vostok.hercules.sentry.client.impl.v9.connector;

import io.sentry.connection.ConnectionException;
import io.sentry.connection.EventSampler;
import io.sentry.connection.HttpConnection;
import io.sentry.event.Event;

import java.net.Proxy;
import java.net.URL;

/**
 * Extends {@link HttpConnection} for sending without locking
 *
 * @author Petr Demenev
 */
public class HerculesHttpConnection extends HttpConnection {

    public HerculesHttpConnection(URL sentryUrl, String publicKey, String secretKey, Proxy proxy, EventSampler eventSampler) {
        super(sentryUrl, publicKey, secretKey, proxy, eventSampler);
    }

    /**
     * Overrides {@link io.sentry.connection.AbstractConnection#send(Event)}
     * excluding locking and handling of unused eventSendCallbacks
     *
     * @param event event for sending to Sentry
     * @throws ConnectionException in case of error of sending to Sentry
     */
    public void sendWithoutLocking(Event event) throws ConnectionException {
        doSend(event);
    }
}
