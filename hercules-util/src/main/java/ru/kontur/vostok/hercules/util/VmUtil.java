package ru.kontur.vostok.hercules.util;

import sun.misc.VM;

/**
 * @author Gregory Koshelev
 */
public final class VmUtil {
    /**
     * Return the maximum amount of allocatable memory for direct buffers.
     *
     * @return the maximum direct memory
     */
    public static long maxDirectMemory() {
        return VM.maxDirectMemory();/* FIXME: implementation specific API is used */
    }

    private VmUtil() {
        /* static class */
    }
}
