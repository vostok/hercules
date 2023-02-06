package ru.kontur.vostok.hercules.radamant.rules;

import java.util.Map;
import ru.kontur.vostok.hercules.protocol.TinyString;
import ru.kontur.vostok.hercules.protocol.Variant;

/**
 * Rule for metric aggregation.
 *
 * @author Tatyana Tokmyanina
 */
public interface Rule {
    Map<TinyString, Variant> getEnrichment();
}
