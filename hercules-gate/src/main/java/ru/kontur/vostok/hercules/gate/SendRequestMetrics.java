package ru.kontur.vostok.hercules.gate;

import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.health.Timer;
import ru.kontur.vostok.hercules.util.time.TimeSource;

/**
 * @author Gregory Koshelev
 */
public class SendRequestMetrics {
    private final TimeSource time;

    private final Timer recvTimeMsTimer;
    private final Timer decompressionTimeMsTimer;
    private final Timer processingTimeMsTimer;

    public SendRequestMetrics(MetricsCollector metricsCollector, TimeSource time) {
        this.time = time;

        this.recvTimeMsTimer = metricsCollector.timer(this.getClass().getSimpleName() + ".recvTimeMs");
        this.decompressionTimeMsTimer = metricsCollector.timer(this.getClass().getSimpleName() + ".decompressionTimeMs");
        this.processingTimeMsTimer = metricsCollector.timer(this.getClass().getSimpleName() + ".processingTimeMs");
    }

    public void update(SendRequestProcessor.SendRequest request, int code) {
        recvTimeMsTimer.update(request.receivingTimeMs());
        decompressionTimeMsTimer.update(request.decompressionTimeMs());
        processingTimeMsTimer.update(request.processingTimeMs());
    }
}
