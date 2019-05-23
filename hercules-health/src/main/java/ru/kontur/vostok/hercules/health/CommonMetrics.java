package ru.kontur.vostok.hercules.health;

import ru.kontur.vostok.hercules.util.collection.CollectionUtil;
import ru.kontur.vostok.hercules.util.metrics.GraphiteMetricsUtil;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * CommonMetrics - set of common metrics
 *
 * @author Kirill Sulim
 */
public final class CommonMetrics {

    private CommonMetrics() {
        /* static class */
    }

    public static void registerCommonMetrics(MetricsCollector metricsCollector) {
        registerMemoryMetrics(metricsCollector);
        registerSystemMetrics(metricsCollector);
        registerGarbageCollectionMetrics(metricsCollector);
        registerThreadsMetrics(metricsCollector);
    }

    public static void registerCommonMetrics(MetricsCollector metricsCollector, String ... patterns) {
        registerMemoryMetrics(metricsCollector);
        registerSystemMetrics(metricsCollector);
        registerGarbageCollectionMetrics(metricsCollector);
        registerThreadsMetrics(metricsCollector, patterns);
    }

    public static void registerMemoryMetrics(MetricsCollector metricsCollector) {
        final Runtime runtime = Runtime.getRuntime();
        metricsCollector.gauge("memory.totalMemoryBytes", runtime::totalMemory);
        metricsCollector.gauge("memory.freeMemoryBytes", runtime::freeMemory);
        metricsCollector.gauge("memory.usedMemoryBytes", () -> runtime.totalMemory() - runtime.freeMemory());
        metricsCollector.gauge("memory.maxMemoryBytes", runtime::maxMemory);
        metricsCollector.gauge("memory.totalFreeMemoryBytes", () -> runtime.maxMemory() - runtime.totalMemory() + runtime.freeMemory());

        final MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        if (Objects.nonNull(memoryMXBean)) {
            metricsCollector.gauge("memory.heapMaxBytes", () -> memoryMXBean.getHeapMemoryUsage().getMax());
            metricsCollector.gauge("memory.heapUsedBytes", () -> memoryMXBean.getHeapMemoryUsage().getUsed());
            metricsCollector.gauge("memory.nonHeapMaxBytes", () -> memoryMXBean.getNonHeapMemoryUsage().getMax());
            metricsCollector.gauge("memory.nonHeapUsedBytes", () -> memoryMXBean.getNonHeapMemoryUsage().getUsed());
        }
    }

    public static void registerSystemMetrics(MetricsCollector metricsCollector) {
        final OperatingSystemMXBean operatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean();
        if (Objects.nonNull(operatingSystemMXBean)) {
            metricsCollector.gauge("os.loadAverage", operatingSystemMXBean::getSystemLoadAverage);
        }
    }

    public static void registerGarbageCollectionMetrics(MetricsCollector metricsCollector) {
        for (GarbageCollectorMXBean garbageCollectorMXBean : CollectionUtil.nonNullElseEmpty(ManagementFactory.getGarbageCollectorMXBeans())) {
            String collectorName = garbageCollectorMXBean.getName();
            metricsCollector.gauge("gc." + collectorName + ".collectionCount", garbageCollectorMXBean::getCollectionCount);
            metricsCollector.gauge("gc." + collectorName + ".collectionTimeMs", garbageCollectorMXBean::getCollectionTime);
        }
    }

    public static void registerThreadsMetrics(MetricsCollector metricsCollector, String ... patterns) {
        final List<Pattern> compiledPatterns = Arrays.stream(patterns).map(Pattern::compile).collect(Collectors.toList());

        final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        if (Objects.nonNull(threadMXBean)) {
            for (Thread.State state : Thread.State.values()) {
                metricsCollector.gauge(
                        "threads.states." + state.name() + ".count",
                        () -> getThreadCount(threadMXBean, state)
                );

                for (Pattern pattern : compiledPatterns) {
                    final String patternMetricName = GraphiteMetricsUtil.sanitizeMetricName(pattern.pattern());
                    metricsCollector.gauge(
                            "threads.pattern." + patternMetricName + ".state." + state.name() + ".count",
                            () -> getThreadCount(threadMXBean, pattern, state)
                    );
                }
            }
        }
    }

    private static long getThreadCount(ThreadMXBean threadMXBean, Thread.State state) {
        return getThreadsInfo(threadMXBean)
                .filter(ti -> ti.getThreadState().equals(state))
                .count();
    }

    private static long getThreadCount(ThreadMXBean threadMXBean, Pattern pattern, Thread.State state) {
        return getThreadsInfo(threadMXBean)
                .filter(ti -> pattern.matcher(ti.getThreadName()).matches() && ti.getThreadState().equals(state))
                .count();
    }

    private static Stream<ThreadInfo> getThreadsInfo(ThreadMXBean threadMXBean) {
        return Arrays.stream(threadMXBean.getThreadInfo(threadMXBean.getAllThreadIds()));
    }
}
