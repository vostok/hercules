package ru.kontur.vostok.hercules.gate;

import ru.kontur.vostok.hercules.util.bytes.SizeUnit;

/**
 * @author Gregory Koshelev
 */
public final class GateDefaults {
    public static final String DEFAULT_HOST = "0.0.0.0";
    public static final int DEFAULT_PORT = 6306;
    public static final long MAX_CONTENT_LENGTH = SizeUnit.MEGABYTES.toBytes(4);

    private GateDefaults() {
    }
}
