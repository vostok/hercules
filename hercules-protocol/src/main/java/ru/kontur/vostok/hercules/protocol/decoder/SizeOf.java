package ru.kontur.vostok.hercules.protocol.decoder;

/**
 * @deprecated Use {@link ru.kontur.vostok.hercules.protocol.Type#size} instead
 */
@Deprecated
public final class SizeOf {
    public static final int BYTE = 1;
    public static final int SHORT = 2;
    public static final int INTEGER = 4;
    public static final int LONG = 8;
    public static final int FLAG = 1;
    public static final int FLOAT = 4;
    public static final int DOUBLE = 8;
    public static final int UUID = 16;
    public static final int TYPE = 1;

    private SizeOf() {
    }
}
