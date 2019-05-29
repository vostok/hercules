package ru.kontur.vostok.hercules.gate.client;

/**
 * @author Gregory Koshelev
 */
public final class GateClientDefaults {
    private GateClientDefaults() {
    }

    public static final int DEFAULT_TIMEOUT = 30_000;
    public static final int DEFAULT_CONNECTION_COUNT = 1_000;
    public static final int DEFAULT_RECOVERY_TIME = 10_000;
}
