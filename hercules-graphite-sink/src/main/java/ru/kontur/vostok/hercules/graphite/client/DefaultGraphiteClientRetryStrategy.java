package ru.kontur.vostok.hercules.graphite.client;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * DefaultGraphiteClientRetryStrategy
 *
 * @author Kirill Sulim
 */
public class DefaultGraphiteClientRetryStrategy extends SimpleGraphiteClientRetryStrategy {

    private static final int DEFAULT_RETRY_COUNT = 3;
    private static final Set<Class<? extends Exception>> DEFAULT_SUPPRESSED_EXCEPTIONS = new HashSet<>(Arrays.asList(
            IOException.class
    ));

    public DefaultGraphiteClientRetryStrategy(GraphiteMetricDataSender sender) {
        super(sender, DEFAULT_RETRY_COUNT, DEFAULT_SUPPRESSED_EXCEPTIONS);
    }
}
