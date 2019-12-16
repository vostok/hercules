package ru.kontur.vostok.hercules.sink.filter;

import ru.kontur.vostok.hercules.protocol.Event;

import java.util.Properties;

/**
 * @author Gregory Koshelev
 */
public class WhitelistEventFilter extends EventFilter {
    private final BlacklistEventFilter blacklistEventFilter;

    /**
     * Inheritors must implement constructor with the same signature.
     *
     * @param properties properties for the filter initialization
     */
    protected WhitelistEventFilter(Properties properties) {
        super(properties);
        blacklistEventFilter = new BlacklistEventFilter(properties);
    }

    @Override
    public boolean test(Event event) {
        return !blacklistEventFilter.test(event);
    }
}
