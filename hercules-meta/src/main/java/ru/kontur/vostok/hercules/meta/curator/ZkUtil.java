package ru.kontur.vostok.hercules.meta.curator;

/**
 * @author Gregory Koshelev
 */
public final class ZkUtil {
    public static String getLeafNodeFromPath(String path) {
        int offset = path.lastIndexOf('/');
        if (offset == -1) {
            return null;
        }
        return path.substring(offset + 1);
    }

    private ZkUtil() {
        /* static class */
    }
}
