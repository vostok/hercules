package ru.kontur.vostok.hercules.health;

import ru.kontur.vostok.hercules.util.VmUtil;

import java.lang.management.BufferPoolMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryType;
import java.util.EnumMap;
import java.util.Map;

/**
 * @author Gregory Koshelev
 */
public final class MemoryMetrics {
    private static final Map<MemoryType, String> memoryTypes;

    static {
        Map<MemoryType, String> map = new EnumMap<>(MemoryType.class);

        map.put(MemoryType.HEAP, "heap");
        map.put(MemoryType.NON_HEAP, "non-heap");

        memoryTypes = map;
    }

    public static void register(MetricsCollector collector) {
        registerHeapMemoryMetrics(collector);
        registerNonHeapMemoryMetrics(collector);
        registerInternalNonHeapMemoryMetrics(collector);
        registerBufferNonHeapMemoryMetrics(collector);
    }

    /**
     * Register metrics of heap memory usage by runtime. Values are obtained from {@link Runtime}.
     *
     * @param collector metrics collector
     */
    private static void registerHeapMemoryMetrics(MetricsCollector collector) {
        String metricPrefix = MetricsUtil.toMetricPath("memory", "heap", "runtime");
        Runtime runtime = Runtime.getRuntime();

        collector.gauge(MetricsUtil.toMetricPathWithPrefix(metricPrefix, "free-bytes"), runtime::freeMemory);
        collector.gauge(MetricsUtil.toMetricPathWithPrefix(metricPrefix, "used-bytes"), () -> runtime.totalMemory() - runtime.freeMemory());
        collector.gauge(MetricsUtil.toMetricPathWithPrefix(metricPrefix, "total-bytes"), runtime::totalMemory);
        collector.gauge(MetricsUtil.toMetricPathWithPrefix(metricPrefix, "max-bytes"), runtime::maxMemory);
    }

    /**
     * Register metric of max direct memory is available for runtime.
     *
     * @param collector metrics collector
     */
    private static void registerNonHeapMemoryMetrics(MetricsCollector collector) {
        collector.gauge(MetricsUtil.toMetricPath("memory", "non-heap", "runtime", "max-bytes"), VmUtil::maxDirectMemory);
    }

    /**
     * Register metrics of non-heap memory usage by runtime for internal purposes (Code Cache, Metaspace, ...).
     *
     * @param collector metrics collector
     */
    private static void registerInternalNonHeapMemoryMetrics(MetricsCollector collector) {
        String metricPrefix = MetricsUtil.toMetricPath("memory", "non-heap", "internal");
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();

        /* Also, it is possible to get this value as sum of MemoryPoolMXBean.getUsage().getUsed() over list ManagementFactory.getMemoryPoolMXBeans() */
        collector.gauge(MetricsUtil.toMetricPathWithPrefix(metricPrefix, "used-bytes"), () -> memoryBean.getNonHeapMemoryUsage().getUsed());
        collector.gauge(MetricsUtil.toMetricPathWithPrefix(metricPrefix, "max-bytes"), () -> memoryBean.getNonHeapMemoryUsage().getMax());
    }

    /**
     * Register metrics of non-heap memory usage by native buffers.
     * See {@link java.nio.ByteBuffer#allocateDirect(int)} and {@link java.nio.MappedByteBuffer}.
     *
     * @param collector metrics collector
     */
    private static void registerBufferNonHeapMemoryMetrics(MetricsCollector collector) {
        String metricPrefix = MetricsUtil.toMetricPath("memory", "non-heap");
        for (BufferPoolMXBean bean : ManagementFactory.getPlatformMXBeans(BufferPoolMXBean.class)) {
            collector.gauge(MetricsUtil.toMetricPathWithPrefix(metricPrefix, bean.getName(), "count"), bean::getCount);
            collector.gauge(MetricsUtil.toMetricPathWithPrefix(metricPrefix, bean.getName(), "used-bytes"), bean::getMemoryUsed);
            collector.gauge(MetricsUtil.toMetricPathWithPrefix(metricPrefix, bean.getName(), "capacity-bytes"), bean::getTotalCapacity);
        }
    }

    private MemoryMetrics() {
        /* static class */
    }
}
