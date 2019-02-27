package ru.kontur.hercules.tracing.api;

/**
 * PageInfo
 *
 * @author Kirill Sulim
 */
public class PageInfo {

    private final long offset;
    private final int count;

    public PageInfo(long offset, int count) {
        this.offset = offset;
        this.count = count;
    }

    public long getOffset() {
        return offset;
    }

    public int getCount() {
        return count;
    }

    public static PageInfo of(final long offset, final int count) {
        return new PageInfo(offset, count);
    }
}
