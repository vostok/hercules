package ru.kontur.vostok.hercules.util;

import ru.kontur.vostok.hercules.util.bytes.SizeUnit;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.List;

/**
 * @author Gregory Koshelev
 */
public final class VmUtil {
    private static final long maxDirectMemory;

    static {
        long maxDirectMemorySize = Runtime.getRuntime().maxMemory();

        RuntimeMXBean bean = ManagementFactory.getRuntimeMXBean();
        List<String> args = bean.getInputArguments();
        for (String arg : args) {
            if (arg.startsWith("-XX:MaxDirectMemorySize=")) {
                String size = arg.substring("-XX:MaxDirectMemorySize=".length());
                maxDirectMemorySize = parseSize(size, Runtime.getRuntime().maxMemory());
            }
        }

        maxDirectMemory = maxDirectMemorySize;
    }

    /**
     * Return the maximum amount of allocatable memory for direct buffers.
     *
     * @return the maximum direct memory
     */
    public static long maxDirectMemory() {
        return maxDirectMemory;
    }

    private static long parseSize(String size, long defaultSize) {
        if (size.length() <= 1) {
            return defaultSize;
        }

        SizeUnit unit = SizeUnit.BYTES;
        switch (size.charAt(size.length() - 1)) {
            case 'g':
            case 'G':
                unit = SizeUnit.GIGABYTES;
                break;
            case 'm':
            case 'M':
                unit = SizeUnit.MEGABYTES;
                break;
            case 'k':
            case 'K':
                unit = SizeUnit.KILOBYTES;
                break;
        }

        long value;
        try {
            value = Long.parseLong(size.substring(0, size.length() - 1));
        } catch (NumberFormatException ex) {
            return defaultSize;
        }

        return unit.toBytes(value);
    }

    private VmUtil() {
        /* static class */
    }
}
