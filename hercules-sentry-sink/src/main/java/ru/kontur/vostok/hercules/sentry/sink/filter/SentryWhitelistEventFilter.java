package ru.kontur.vostok.hercules.sentry.sink.filter;

import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.sink.filter.EventFilter;

import java.util.Properties;

/**
 * Sentry whitelist event filter uses patterns to filter out any events with tag values which don't match any of patterns.
 * <p>
 * This filter patterns may be initialized as well as in {@link SentryBlacklistEventFilter}.
 *
 * @author Petr Demenev
 */
public class SentryWhitelistEventFilter extends EventFilter {
    private final SentryBlacklistEventFilter sentryBlacklistEventFilter;

    /**
     * Inheritors must implement constructor with the same signature.
     *
     * @param properties properties for the filter initialization
     */
    public SentryWhitelistEventFilter(Properties properties) {
        super(properties);
        sentryBlacklistEventFilter = new SentryBlacklistEventFilter(properties);
    }

    @Override
    public boolean test(Event event) {
        return !sentryBlacklistEventFilter.test(event);
    }
}
