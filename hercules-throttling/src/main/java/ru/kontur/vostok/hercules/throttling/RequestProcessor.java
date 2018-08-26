package ru.kontur.vostok.hercules.throttling;

/**
 * Request processor is used to asynchronous process request under throttling
 *
 * @author Gregory Koshelev
 */
public interface RequestProcessor<R, C> {
    /**
     * Asynchronously process request with context data. Method calls callback function when used resources are freed
     * @param request to be processed
     * @param context is additional request's data
     * @param callback function is called to signal throttle that required resource are successfully freed
     */
    void processAsync(R request, C context, ThrottleCallback callback);
}
