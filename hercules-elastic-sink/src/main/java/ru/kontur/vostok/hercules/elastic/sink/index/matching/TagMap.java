package ru.kontur.vostok.hercules.elastic.sink.index.matching;

import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.Type;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.protocol.hpath.HPath;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * @author Anton Akkuzin
 */
public class TagMap {
    private final Map<HPath, Pattern> tagMap;

    public TagMap(Map<HPath, Pattern> tagMap) {
        this.tagMap = tagMap;
    }

    /**
     * Checks the given {@link Event} matches to all patterns.
     *
     * @param event event to test
     * @return {@code true} if {@code event} tags match to all patterns.
     */
    public boolean matches(Event event) {
        for (Map.Entry<HPath, Pattern> tagMapPair : tagMap.entrySet()) {
            Variant tag = tagMapPair.getKey().extract(event.getPayload());
            if (tag == null) {
                return false;
            }
            if (!tag.getType().isPrimitive()) {
                return false;
            }
            Pattern patternTagValue = tagMapPair.getValue();
            if (!patternTagValue.matcher(stringOf(tag)).matches()) {
                return false;
            }
        }
        return true;
    }

    private String stringOf(Variant variant) {
        return variant.getType() == Type.STRING
                ? new String((byte[]) variant.getValue(), StandardCharsets.UTF_8)
                : String.valueOf(variant.getValue());
    }
}
