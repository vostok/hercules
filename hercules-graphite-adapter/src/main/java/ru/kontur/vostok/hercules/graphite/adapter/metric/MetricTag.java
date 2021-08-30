package ru.kontur.vostok.hercules.graphite.adapter.metric;

/**
 * The metric tag is a kye-value pair.
 * <p>
 * The key and value is an UTF8-encoded strings.
 *
 * @author Gregory Koshelev
 */
public class MetricTag {
    private final byte[] key;
    private final byte[] value;

    public MetricTag(byte[] key, byte[] value) {
        this.key = key;
        this.value = value;
    }

    /**
     * The tag key.
     * <p>
     * Returns UTF8-encoded bytes.
     *
     * @return the tag key
     */
    public byte[] key() {
        return key;
    }

    /**
     * The tag value.
     * <p>
     * Returns UTF8-encoded bytes.
     *
     * @return the tag value
     */
    public byte[] value() {
        return value;
    }
}
