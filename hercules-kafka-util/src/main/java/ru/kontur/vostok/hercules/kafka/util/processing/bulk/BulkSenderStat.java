package ru.kontur.vostok.hercules.kafka.util.processing.bulk;

/**
 * BulkSenderStat
 *
 * @author Kirill Sulim
 */
public class BulkSenderStat {

    public static final BulkSenderStat ZERO = new BulkSenderStat(0, 0);

    private final int processed;
    private final int dropped;

    public BulkSenderStat(int processed, int dropped) {
        this.processed = processed;
        this.dropped = dropped;
    }

    public int getProcessed() {
        return processed;
    }

    public int getDropped() {
        return dropped;
    }
}
