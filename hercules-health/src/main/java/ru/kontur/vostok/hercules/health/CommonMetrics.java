package ru.kontur.vostok.hercules.health;

/**
 * CommonMetrics - set of common metrics
 *
 * @author Kirill Sulim
 */
public final class CommonMetrics {

    public static void registerMemoryMetrics(MetricsCollector metricsCollector) {
        final Runtime runtime = Runtime.getRuntime();
        metricsCollector.gauge("gc.totalMemoryBytes", runtime::totalMemory);
        metricsCollector.gauge("gc.freeMemoryBytes", runtime::freeMemory);
        metricsCollector.gauge("gc.usedMemoryBytes", () -> runtime.totalMemory() - runtime.freeMemory());
        metricsCollector.gauge("gc.maxMemoryBytes", runtime::maxMemory);
        metricsCollector.gauge("gc.totalFreeMemoryBytes", () -> runtime.maxMemory() - runtime.totalMemory() + runtime.freeMemory());
    }

    private CommonMetrics() {
        /* static class */
    }
}
