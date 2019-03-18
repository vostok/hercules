package ru.kontur.vostok.hercules.throttling;

import ru.kontur.vostok.hercules.util.bytes.SizeUnit;

/**
 * @author Gregory Koshelev
 */
public final class ThrottlingDefaults {
    public static final long DEFAULT_CAPACITY = 100_000_000L;
    public static final SizeUnit DEFAULT_CAPACITY_UNIT = SizeUnit.BYTES;
    public static final long DEFAULT_REQUEST_TIMEOUT = 5_000L;

    private ThrottlingDefaults() {}
}
