package ru.kontur.vostok.hercules.management.api;

import ru.kontur.vostok.hercules.http.HttpServerRequest;
import ru.kontur.vostok.hercules.http.HttpStatusCodes;
import ru.kontur.vostok.hercules.http.query.QueryUtil;
import ru.kontur.vostok.hercules.meta.task.TaskFuture;

/**
 * Helper class for async HTTP API
 *
 * @author Gregory Koshelev
 */
public class HttpAsyncApiHelper {
    /**
     * Complete HTTP request immediately if async API is called without task awaiting.
     * Otherwise, await task and complete request after that.
     * <p>
     * Possible status codes:<br>
     * {@link HttpStatusCodes#INTERNAL_SERVER_ERROR} - task is failed<br>
     * {@link HttpStatusCodes#REQUEST_TIMEOUT} - non async API was called and task did not completed for time quota<br>
     * {@link HttpStatusCodes#OK} - async API was called or task successfully completed<br>
     *
     * @param task    the task
     * @param request the HTTP request
     */
    public static void awaitAndComplete(TaskFuture task, HttpServerRequest request) {
        if (task.isFailed()) {
            request.complete(HttpStatusCodes.INTERNAL_SERVER_ERROR);
            return;
        }

        if (QueryUtil.get(QueryParameters.ASYNC, request).isEmpty()) {
            task.await();
            if (task.isDone()) {
                request.complete(HttpStatusCodes.OK);
                return;
            }
            request.complete(HttpStatusCodes.REQUEST_TIMEOUT);
            return;
        }
        request.complete(HttpStatusCodes.OK);
    }
}
