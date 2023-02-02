package ru.kontur.vostok.hercules.http.handler;

import ru.kontur.vostok.hercules.application.Application;
import ru.kontur.vostok.hercules.application.ApplicationState;
import ru.kontur.vostok.hercules.http.HttpServerRequest;
import ru.kontur.vostok.hercules.http.HttpStatusCodes;

/**
 * @author Gregory Koshelev
 */
public class StatusHandler implements HttpHandler {
    @Override
    public void handle(HttpServerRequest request) {
        request.complete(statusCodeFromState(Application.application().getState()));
    }

    private int statusCodeFromState(ApplicationState state) {
        switch (state) {
            case STARTING:
                return HttpStatusCodes.SERVICE_UNAVAILABLE;
            case RUNNING:
                return HttpStatusCodes.OK;
            case STOPPING:
                return HttpStatusCodes.GONE;
            default:
                return HttpStatusCodes.INTERNAL_SERVER_ERROR;
        }
    }
}
